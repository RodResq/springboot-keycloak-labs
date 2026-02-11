package br.jus.tjpa.jsecurity.controller;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/client-scope-association")
public class ClientScopeAssociationController {

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    @Value("${keycloak.user-client}")
    private String clientId;

    @Value("${keycloak.client-scope-name:ciprej-app}")
    private String clientScopeName;

    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificarAssociacao() {
        Map<String, Object> response = new HashMap<>();

        try {
            ClientRepresentation client = keycloak.realm(targetRealm)
                    .clients()
                    .findByClientId(clientId)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (client == null) {
                response.put("status", "ERRO");
                response.put("mensagem", "Client '" + clientId + "' não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            log.info("Client encontrado: {}", client.getClientId());

            // 2. Buscar o client scope
            ClientScopeRepresentation clientScope = keycloak.realm(targetRealm)
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals(cs.getName()))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                response.put("status", "ERRO");
                response.put("mensagem", "Client Scope '" + clientScopeName + "' não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            log.info("Client Scope encontrado: {}", clientScope.getName());

            // 3. Verificar default scopes
            Map<String, ClientScopeRepresentation> defaultScopes = (Map<String, ClientScopeRepresentation>) keycloak.realm(targetRealm)
                    .clients()
                    .get(client.getId())
                    .getDefaultClientScopes();

            boolean isDefault = defaultScopes.values().stream()
                    .anyMatch(cs -> cs.getId().equals(clientScope.getId()));

            Map<String, ClientScopeRepresentation> optionalScopes = (Map<String, ClientScopeRepresentation>) keycloak.realm(targetRealm)
                    .clients()
                    .get(client.getId())
                    .getOptionalClientScopes();

            boolean isOptional = optionalScopes.values().stream()
                    .anyMatch(cs -> cs.getId().equals(clientScope.getId()));

            response.put("client_id", client.getClientId());
            response.put("client_uuid", client.getId());
            response.put("client_scope_name", clientScope.getName());
            response.put("client_scope_uuid", clientScope.getId());
            response.put("is_default_scope", isDefault);
            response.put("is_optional_scope", isOptional);
            response.put("total_default_scopes", defaultScopes.size());
            response.put("total_optional_scopes", optionalScopes.size());

            List<String> defaultScopeNames = defaultScopes.values().stream()
                    .map(ClientScopeRepresentation::getName)
                    .collect(Collectors.toList());
            response.put("default_scopes", defaultScopeNames);

            List<String> optionalScopeNames = optionalScopes.values().stream()
                    .map(ClientScopeRepresentation::getName)
                    .collect(Collectors.toList());
            response.put("optional_scopes", optionalScopeNames);

            if (isDefault) {
                response.put("status", "OK");
                response.put("mensagem", "Client Scope está associado como DEFAULT (sempre incluído no token)");
            } else if (isOptional) {
                response.put("status", "AVISO");
                response.put("mensagem", "Client Scope está como OPTIONAL (precisa ser solicitado explicitamente)");
            } else {
                response.put("status", "ERRO");
                response.put("mensagem", "Client Scope NÃO está associado ao client!");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao verificar associação: {}", e.getMessage(), e);
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/associar-default")
    public ResponseEntity<Map<String, Object>> associarComoDefault() {
        Map<String, Object> response = new HashMap<>();

        try {
            ClientRepresentation client = keycloak.realm(targetRealm)
                    .clients()
                    .findByClientId(clientId)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (client == null) {
                response.put("status", "ERRO");
                response.put("mensagem", "Client não encontrado");
                return ResponseEntity.status(404).body(response);
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
                response.put("mensagem", "Client Scope não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            log.info("Client: {}", client.getClientId());
            log.info("Client Scope: {}", clientScope.getName());

            Map<String, ClientScopeRepresentation> defaultScopes = (Map<String, ClientScopeRepresentation>) keycloak.realm(targetRealm)
                    .clients()
                    .get(client.getId())
                    .getDefaultClientScopes();

            boolean jaAssociado = defaultScopes.values().stream()
                    .anyMatch(cs -> cs.getId().equals(clientScope.getId()));

            if (jaAssociado) {
                response.put("status", "JA_EXISTE");
                response.put("mensagem", "Client Scope já está associado como DEFAULT");
                return ResponseEntity.ok(response);
            }

            keycloak.realm(targetRealm)
                    .clients()
                    .get(client.getId())
                    .addDefaultClientScope(clientScope.getId());

            Map<String, ClientScopeRepresentation> scopesAposAssociacao = (Map<String, ClientScopeRepresentation>) keycloak.realm(targetRealm)
                    .clients()
                    .get(client.getId())
                    .getDefaultClientScopes();

            boolean confirmado = scopesAposAssociacao.values().stream()
                    .anyMatch(cs -> cs.getId().equals(clientScope.getId()));

            if (confirmado) {
                response.put("status", "SUCESSO");
                response.put("mensagem", "Client Scope associado como DEFAULT com sucesso!");
            } else {
                response.put("status", "INCERTO");
                response.put("mensagem", "Comando executado mas associação não confirmada");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/remover")
    public ResponseEntity<Map<String, Object>> removerAssociacao() {
        Map<String, Object> response = new HashMap<>();

        try {
            ClientRepresentation client = keycloak.realm(targetRealm)
                    .clients()
                    .findByClientId(clientId)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (client == null) {
                response.put("status", "ERRO");
                response.put("mensagem", "Client não encontrado");
                return ResponseEntity.status(404).body(response);
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
                response.put("mensagem", "Client Scope não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            keycloak.realm(targetRealm)
                    .clients()
                    .get(client.getId())
                    .removeDefaultClientScope(clientScope.getId());

            log.info("Associação removida");

            response.put("status", "REMOVIDO");
            response.put("mensagem", "Associação removida com sucesso");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao remover associação: {}", e.getMessage(), e);
            response.put("status", "ERRO");
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


}
