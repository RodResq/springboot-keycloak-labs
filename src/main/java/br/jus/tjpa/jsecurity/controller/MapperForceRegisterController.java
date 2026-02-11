package br.jus.tjpa.jsecurity.controller;

import br.jus.tjpa.jsecurity.config.CpfProtocolMapperConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mapper")
public class MapperForceRegisterController {

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private CpfProtocolMapperConfiguration cpfMapperConfig;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    @Value("${keycloak.client-scope-name:acme}")
    private String clientScopeName;

    @PostMapping("/forcar-registro-cpf")
    public ResponseEntity<Map<String, Object>> forcarRegistroCpf() {
        Map<String, Object> response = new HashMap<>();
        List<String> logs = new ArrayList<>();

        try {
            if (cpfMapperConfig == null) {
                response.put("status", "ERRO");
                response.put("logs", logs);
                return ResponseEntity.status(500).body(response);
            }

            ClientScopeRepresentation clientScope = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                response.put("status", "ERRO");
                response.put("logs", logs);
                return ResponseEntity.status(404).body(response);
            }

            ProtocolMapperRepresentation representation = new ProtocolMapperRepresentation();
            cpfMapperConfig.configure(representation);

            ClientScopeResource clientScopeResource = keycloak.realm(targetRealm)
                    .clientScopes()
                    .get(clientScope.getId());

            boolean mapperExists = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> representation.getName().equals(m.getName()));

            if (mapperExists) {
                response.put("status", "JA_EXISTE");
                response.put("logs", logs);
                return ResponseEntity.ok(response);
            }

            clientScopeResource.getProtocolMappers().createMapper(representation).close();

            boolean created = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> representation.getName().equals(m.getName()));

            if (created) {
                ProtocolMapperRepresentation createdMapper = clientScopeResource
                        .getProtocolMappers()
                        .getMappers()
                        .stream()
                        .filter(m -> representation.getName().equals(m.getName()))
                        .findFirst()
                        .orElse(null);

                if (createdMapper != null) {
                    response.put("mapper_id", createdMapper.getId());
                    response.put("mapper_details", Map.of(
                            "id", createdMapper.getId(),
                            "name", createdMapper.getName(),
                            "protocol", createdMapper.getProtocol(),
                            "type", createdMapper.getProtocolMapper(),
                            "config", createdMapper.getConfig()
                    ));
                }

                response.put("status", "SUCESSO");
            } else {
                response.put("status", "INCERTO");
            }

            response.put("logs", logs);
            response.put("client_scope_id", clientScope.getId());
            response.put("client_scope_name", clientScope.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERRO");
            response.put("logs", logs);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    @DeleteMapping("/deletar-mapper-cpf")
    public ResponseEntity<Map<String, Object>> deletarMapperCpf() {
        Map<String, Object> response = new HashMap<>();

        try {
            ClientScopeRepresentation clientScope = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                response.put("status", "ERRO");
                response.put("mensagem", "Client Scope não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            ClientScopeResource clientScopeResource = keycloak.realm(targetRealm)
                    .clientScopes()
                    .get(clientScope.getId());

            ProtocolMapperRepresentation cpfMapper = clientScopeResource
                    .getProtocolMappers()
                    .getMappers()
                    .stream()
                    .filter(m -> "cpf".equals(m.getName()))
                    .findFirst()
                    .orElse(null);

            if (cpfMapper == null) {
                response.put("status", "NAO_ENCONTRADO");
                response.put("mensagem", "Mapper CPF não existe");
                return ResponseEntity.ok(response);
            }

            clientScopeResource.getProtocolMappers()
                    .delete(cpfMapper.getId());

            response.put("status", "DELETADO");
            response.put("mensagem", "Mapper CPF deletado com sucesso");
            response.put("mapper_id", cpfMapper.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/listar-mappers")
    public ResponseEntity<Map<String, Object>> listarMappers() {
        Map<String, Object> response = new HashMap<>();

        try {
            ClientScopeRepresentation clientScope = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                response.put("status", "ERRO");
                response.put("mensagem", "Client Scope não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            List<ProtocolMapperRepresentation> mappers = keycloak.realm(targetRealm)
                    .clientScopes()
                    .get(clientScope.getId())
                    .getProtocolMappers()
                    .getMappers();

            response.put("client_scope_id", clientScope.getId());
            response.put("client_scope_name", clientScope.getName());
            response.put("total_mappers", mappers.size());

            List<Map<String, Object>> mappersList = new ArrayList<>();
            for (ProtocolMapperRepresentation mapper : mappers) {
                Map<String, Object> mapperInfo = new HashMap<>();
                mapperInfo.put("id", mapper.getId());
                mapperInfo.put("name", mapper.getName());
                mapperInfo.put("protocol", mapper.getProtocol());
                mapperInfo.put("type", mapper.getProtocolMapper());
                mapperInfo.put("config", mapper.getConfig());
                mappersList.add(mapperInfo);
            }

            response.put("mappers", mappersList);
            response.put("status", "OK");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
