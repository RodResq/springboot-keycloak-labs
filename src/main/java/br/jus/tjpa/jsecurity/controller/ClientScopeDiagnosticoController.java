package br.jus.tjpa.jsecurity.controller;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/diagnostico/client-scopes")
public class ClientScopeDiagnosticoController {

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    @Value("${keycloak.client-scope-name:acme-dedicated}")
    private String clientScopeName;

    /**
     * Lista todos os client scopes disponíveis
     */
    @GetMapping("/listar")
    public ResponseEntity<Map<String, Object>> listarClientScopes() {
        log.info("=== LISTANDO CLIENT SCOPES ===");

        try {
            List<ClientScopeRepresentation> clientScopes = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("total", clientScopes.size());
            response.put("realm", targetRealm);
            response.put("client_scope_configurado", clientScopeName);

            List<Map<String, Object>> scopesList = clientScopes.stream()
                    .map(cs -> {
                        Map<String, Object> scopeInfo = new HashMap<>();
                        scopeInfo.put("id", cs.getId());
                        scopeInfo.put("name", cs.getName());
                        scopeInfo.put("protocol", cs.getProtocol());
                        scopeInfo.put("description", cs.getDescription());

                        // Verificar se é o scope configurado
                        if (clientScopeName.equals(cs.getName())) {
                            scopeInfo.put("is_target", true);
                            log.info("✓ Client Scope alvo encontrado: {}", cs.getName());
                        } else {
                            scopeInfo.put("is_target", false);
                        }

                        return scopeInfo;
                    })
                    .collect(Collectors.toList());

            response.put("client_scopes", scopesList);

            // Verificar se o scope configurado existe
            boolean targetExists = clientScopes.stream()
                    .anyMatch(cs -> clientScopeName.equals(cs.getName()));

            if (targetExists) {
                response.put("status", "OK");
                response.put("mensagem", "Client Scope configurado encontrado");
            } else {
                response.put("status", "ERRO");
                response.put("mensagem", "Client Scope '" + clientScopeName + "' NÃO encontrado!");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao listar client scopes: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verifica os mappers do client scope configurado
     */
    @GetMapping("/mappers")
    public ResponseEntity<Map<String, Object>> verificarMappers() {
        log.info("=== VERIFICANDO MAPPERS DO CLIENT SCOPE ===");
        log.info("Client Scope: {}", clientScopeName);

        try {
            // Buscar o client scope
            ClientScopeRepresentation clientScope = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                log.error("❌ Client Scope '{}' não encontrado!", clientScopeName);
                return ResponseEntity.status(404).body(Map.of(
                        "status", "ERRO",
                        "mensagem", "Client Scope não encontrado: " + clientScopeName
                ));
            }

            // Buscar os mappers
            List<ProtocolMapperRepresentation> mappers = keycloak.realm(targetRealm)
                    .clientScopes()
                    .get(clientScope.getId())
                    .getProtocolMappers()
                    .getMappers();

            Map<String, Object> response = new HashMap<>();
            response.put("client_scope_id", clientScope.getId());
            response.put("client_scope_name", clientScope.getName());
            response.put("total_mappers", mappers.size());

            // Verificar se o mapper CPF existe
            boolean cpfMapperExists = mappers.stream()
                    .anyMatch(m -> "cpf".equals(m.getName()));

            response.put("cpf_mapper_existe", cpfMapperExists);

            List<Map<String, Object>> mappersList = mappers.stream()
                    .map(m -> {
                        Map<String, Object> mapperInfo = new HashMap<>();
                        mapperInfo.put("id", m.getId());
                        mapperInfo.put("name", m.getName());
                        mapperInfo.put("protocol", m.getProtocol());
                        mapperInfo.put("protocol_mapper", m.getProtocolMapper());
                        mapperInfo.put("config", m.getConfig());

                        if ("cpf".equals(m.getName())) {
                            mapperInfo.put("is_cpf_mapper", true);
                            log.info("✓✓✓ MAPPER CPF ENCONTRADO!");
                        } else {
                            mapperInfo.put("is_cpf_mapper", false);
                        }

                        return mapperInfo;
                    })
                    .collect(Collectors.toList());

            response.put("mappers", mappersList);

            if (cpfMapperExists) {
                response.put("status", "OK");
                response.put("mensagem", "Mapper CPF encontrado");
            } else {
                response.put("status", "AVISO");
                response.put("mensagem", "Mapper CPF NÃO encontrado. Execute o auto-registro.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao verificar mappers: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Teste completo: verifica tudo
     */
    @GetMapping("/teste-completo")
    public ResponseEntity<Map<String, Object>> testeCompleto() {
        log.info("╔════════════════════════════════════════╗");
        log.info("║        TESTE COMPLETO - INÍCIO         ║");
        log.info("╚════════════════════════════════════════╝");

        Map<String, Object> resultado = new HashMap<>();
        List<String> problemas = new ArrayList<>();
        List<String> sucessos = new ArrayList<>();

        try {
            // 1. Verificar Client Scopes
            log.info("\n[1/3] Verificando Client Scopes...");
            List<ClientScopeRepresentation> allScopes = keycloak.realm("dev")
                    .clientScopes()
                    .findAll();

            resultado.put("total_client_scopes", allScopes.size());

            ClientScopeRepresentation targetScope = allScopes.stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (targetScope != null) {
                sucessos.add("✓ Client Scope '" + clientScopeName + "' encontrado");
                resultado.put("client_scope_existe", true);
                resultado.put("client_scope_id", targetScope.getId());
            } else {
                problemas.add("❌ Client Scope '" + clientScopeName + "' NÃO encontrado");
                resultado.put("client_scope_existe", false);
                resultado.put("status", "ERRO_CRÍTICO");
                resultado.put("problemas", problemas);
                return ResponseEntity.status(404).body(resultado);
            }

            // 2. Verificar Mappers
            log.info("\n[2/3] Verificando Mappers...");
            List<ProtocolMapperRepresentation> mappers = keycloak.realm(targetRealm)
                    .clientScopes()
                    .get(targetScope.getId())
                    .getProtocolMappers()
                    .getMappers();

            resultado.put("total_mappers", mappers.size());

            boolean cpfExists = mappers.stream().anyMatch(m -> "cpf".equals(m.getName()));

            if (cpfExists) {
                sucessos.add("✓ Mapper CPF encontrado");
                resultado.put("cpf_mapper_existe", true);

                // Detalhes do mapper CPF
                ProtocolMapperRepresentation cpfMapper = mappers.stream()
                        .filter(m -> "cpf".equals(m.getName()))
                        .findFirst()
                        .orElse(null);

                if (cpfMapper != null) {
                    Map<String, Object> cpfDetails = new HashMap<>();
                    cpfDetails.put("id", cpfMapper.getId());
                    cpfDetails.put("protocol", cpfMapper.getProtocol());
                    cpfDetails.put("type", cpfMapper.getProtocolMapper());
                    cpfDetails.put("config", cpfMapper.getConfig());
                    resultado.put("cpf_mapper_details", cpfDetails);
                }
            } else {
                problemas.add("❌ Mapper CPF NÃO encontrado");
                resultado.put("cpf_mapper_existe", false);
            }

            // 3. Resumo
            log.info("\n[3/3] Gerando resumo...");
            resultado.put("sucessos", sucessos);
            resultado.put("problemas", problemas);

            if (problemas.isEmpty()) {
                resultado.put("status", "TUDO_OK");
                log.info("\n✓✓✓ TESTE COMPLETO: SUCESSO ✓✓✓");
            } else {
                resultado.put("status", "COM_PROBLEMAS");
                log.warn("\n⚠ TESTE COMPLETO: Problemas encontrados");
            }

            log.info("\n╔════════════════════════════════════════╗");
            log.info("║        TESTE COMPLETO - FIM            ║");
            log.info("╚════════════════════════════════════════╝\n");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("\n❌ ERRO no teste completo: {}", e.getMessage(), e);
            resultado.put("status", "ERRO");
            resultado.put("erro", e.getMessage());
            return ResponseEntity.status(500).body(resultado);
        }
    }
}