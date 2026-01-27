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

    /**
     * Força o registro manual do mapper CPF
     */
    @PostMapping("/forcar-registro-cpf")
    public ResponseEntity<Map<String, Object>> forcarRegistroCpf() {
        log.info("╔════════════════════════════════════════╗");
        log.info("║    FORÇANDO REGISTRO DO MAPPER CPF     ║");
        log.info("╚════════════════════════════════════════╝");

        Map<String, Object> response = new HashMap<>();
        List<String> logs = new ArrayList<>();

        try {
            // 1. Verificar se o bean existe
            logs.add("[1/5] Verificando bean CpfProtocolMapperConfiguration...");
            if (cpfMapperConfig == null) {
                logs.add("❌ ERRO: Bean CpfProtocolMapperConfiguration é NULL!");
                response.put("status", "ERRO");
                response.put("logs", logs);
                return ResponseEntity.status(500).body(response);
            }
            logs.add("✓ Bean encontrado: " + cpfMapperConfig.getClass().getName());

            // 2. Buscar o Client Scope
            logs.add("\n[2/5] Buscando Client Scope '" + clientScopeName + "'...");
            ClientScopeRepresentation clientScope = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                logs.add("❌ ERRO: Client Scope '" + clientScopeName + "' não encontrado!");
                response.put("status", "ERRO");
                response.put("logs", logs);
                return ResponseEntity.status(404).body(response);
            }
            logs.add("✓ Client Scope encontrado:");
            logs.add("  ID: " + clientScope.getId());
            logs.add("  Nome: " + clientScope.getName());

            // 3. Criar a representação do mapper
            logs.add("\n[3/5] Criando representação do mapper...");
            ProtocolMapperRepresentation representation = new ProtocolMapperRepresentation();
            cpfMapperConfig.configure(representation);

            logs.add("✓ Mapper configurado:");
            logs.add("  Nome: " + representation.getName());
            logs.add("  Protocolo: " + representation.getProtocol());
            logs.add("  Tipo: " + representation.getProtocolMapper());
            logs.add("  Configurações: " + representation.getConfig());

            // 4. Verificar se já existe
            ClientScopeResource clientScopeResource = keycloak.realm(targetRealm)
                    .clientScopes()
                    .get(clientScope.getId());

            logs.add("\n[4/5] Verificando se mapper já existe...");
            boolean mapperExists = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> representation.getName().equals(m.getName()));

            if (mapperExists) {
                logs.add("⚠ Mapper '" + representation.getName() + "' já existe!");
                logs.add("  Para recriar, delete primeiro no Keycloak");
                response.put("status", "JA_EXISTE");
                response.put("logs", logs);
                return ResponseEntity.ok(response);
            }
            logs.add("✓ Mapper não existe. Prosseguindo com criação...");

            // 5. Criar o mapper
            logs.add("\n[5/5] Criando mapper no Keycloak...");
            clientScopeResource.getProtocolMappers().createMapper(representation).close();

            // Verificar se foi criado
            boolean created = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> representation.getName().equals(m.getName()));

            if (created) {
                logs.add("✓✓✓ SUCESSO: Mapper CPF criado com sucesso!");

                // Buscar detalhes do mapper criado
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
                logs.add("⚠ AVISO: Comando executado mas mapper não foi encontrado na verificação");
                response.put("status", "INCERTO");
            }

            response.put("logs", logs);
            response.put("client_scope_id", clientScope.getId());
            response.put("client_scope_name", clientScope.getName());

            log.info("\n╔════════════════════════════════════════╗");
            log.info("║       REGISTRO FORÇADO CONCLUÍDO       ║");
            log.info("╚════════════════════════════════════════╝\n");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ ERRO ao forçar registro: {}", e.getMessage(), e);
            logs.add("\n❌ ERRO: " + e.getMessage());
            logs.add("Stack trace: " + Arrays.toString(e.getStackTrace()));
            response.put("status", "ERRO");
            response.put("logs", logs);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Deleta o mapper CPF (para poder recriar)
     */
    @DeleteMapping("/deletar-mapper-cpf")
    public ResponseEntity<Map<String, Object>> deletarMapperCpf() {
        log.info("=== DELETANDO MAPPER CPF ===");

        Map<String, Object> response = new HashMap<>();

        try {
            // Buscar o Client Scope
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

            // Buscar o mapper CPF
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

            // Deletar o mapper
            clientScopeResource.getProtocolMappers()
                    .delete(cpfMapper.getId());

            log.info("✓ Mapper CPF deletado com sucesso");

            response.put("status", "DELETADO");
            response.put("mensagem", "Mapper CPF deletado com sucesso");
            response.put("mapper_id", cpfMapper.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao deletar mapper: {}", e.getMessage(), e);
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Lista todos os mappers do client scope
     */
    @GetMapping("/listar-mappers")
    public ResponseEntity<Map<String, Object>> listarMappers() {
        log.info("=== LISTANDO MAPPERS ===");

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
            log.error("Erro ao listar mappers: {}", e.getMessage(), e);
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
