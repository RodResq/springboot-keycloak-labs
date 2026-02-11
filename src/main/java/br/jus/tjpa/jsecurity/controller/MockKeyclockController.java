package br.jus.tjpa.jsecurity.controller;

import br.jus.tjpa.jsecurity.config.SecurityConfig;
import br.jus.tjpa.jsecurity.model.input.LoginInput;
import br.jus.tjpa.jsecurity.service.SecurityService;
import br.jus.tjpa.jsecurity.service.UserAttributeExtractorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class MockKeyclockController {

    private SecurityConfig securityConfig;

    public MockKeyclockController(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserAttributeExtractorService userAttributeExtractorService;

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;


    @PostMapping("/token")
    public ResponseEntity obtainToken(@Valid @RequestBody LoginInput loginInput) {
        try {
            Keycloak keycloak = securityConfig.getKeycloak();
            AccessTokenResponse accessTokenResponse = keycloak.tokenManager().getAccessToken();
            return ResponseEntity.ok().body(accessTokenResponse);
        } catch (Exception e) {
            return (ResponseEntity<List<String>>) Collections.singletonList("Error: " + e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity login(@Valid @RequestBody LoginInput loginInput) {
        try {
            UserRepresentation userAntes = keycloak.realm(securityConfig.getTargetRealm())
                    .users()
                    .search(loginInput.getUsername())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

            if (userAntes.getAttributes() == null || userAntes.getAttributes().isEmpty()) {
                log.warn("Usuário não possui atributos!");
            } else {
                userAntes.getAttributes().forEach((key, value) -> {
                    log.info("  - {}: {}", key, value);
                });
            }

            UserRepresentation userDepois = keycloak.realm(securityConfig.getTargetRealm())
                    .users()
                    .get(userAntes.getId())
                    .toRepresentation();

            log.info("\nRealizando login no Keycloak...");
            AccessTokenResponse tokenResponse = securityService.login(
                            loginInput.getUsername(),
                            loginInput.getPassword()
            );

            log.info("Token gerado com sucesso");

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokenResponse.getToken());
            response.put("token_type", tokenResponse.getTokenType());
            response.put("expires_in", tokenResponse.getExpiresIn());
            response.put("refresh_token", tokenResponse.getRefreshToken());

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


    @PostMapping("/usuarios/extrair-cpf")
    public ResponseEntity<Map<String, Object>> extrairCpfTodosUsuarios() {
        try {

            userAttributeExtractorService.extractCpfFomAllUses();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Extração de CPF concluída");

            log.info("Processo concluído");
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Erro ao extrair CPF: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/usuario/{username}/extrair-cpf")
    public ResponseEntity<Map<String, Object>> extrairCpfUsuario(@PathVariable String username) {
        try {

            log.info("Extraindo CPF do usuário: {}", username);

            boolean sucesso = userAttributeExtractorService.extractAndSetCPF(username);

            Map<String, Object> response = new HashMap<>();
            if (sucesso) {
                response.put("status", "success");
                response.put("message", "CPF extraído e configurado com sucesso");
                log.info("CPF configurado para: {}", username);
            } else {
                response.put("status", "warning");
                response.put("message", "Não foi possível extrair o CPF");
                log.warn("Falha ao extrair CPF para: {}", username);
            }
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("Erro ao extrair CPF do usuário {}: {}", username, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/keycloak")
    public String getKeycloakFromRequest() {
        try {
            Keycloak keycloak = securityConfig.getKeycloak();
            return keycloak != null ? "Keycloak found": "keycloak is null";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    @GetMapping("/keycloak/clients")
    public ResponseEntity<List<String>> getKeycloakClients() {
        List<String> nomeCLientes = new ArrayList<>();
        try {
            Keycloak keycloak = securityConfig.getKeycloak();
            ClientsResource clientsResource =  keycloak.realm("master").clients();
            for (ClientRepresentation clientRepresentation : clientsResource.findAll()) {
                nomeCLientes.add(clientRepresentation.getClientId());
            }
            return ResponseEntity.ok().body(nomeCLientes);
        } catch (Exception e) {
            return (ResponseEntity<List<String>>) Collections.singletonList("Error: " + e.getMessage());
        }
    }


    @GetMapping("/keycloak/criarRealm")
    public ResponseEntity<String> criarRealm() {
        try {
            Keycloak keycloak = securityConfig.getKeycloak();
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm("criacao-realm-api");
            realm.setEnabled(true);

            keycloak.realms().create(realm);

            return ResponseEntity.ok().body("Realm criado com sucesso");
        } catch (Exception e) {
            return (ResponseEntity<String>) Collections.singletonList("Error: " + e.getMessage());
        }
    }


    @GetMapping("/ldap/users")
    public ResponseEntity<List<Map<String, Object>>> getLdapUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "first", defaultValue = "0") Integer first,
            @RequestParam(name = "max", defaultValue = "10") Integer max) {
        try {
            Keycloak keycloak = securityConfig.getKeycloak();

            List<UserRepresentation> users;

            if (search != null && !search.isEmpty()) {
                users = keycloak.realm(securityConfig.getTargetRealm())
                        .users()
                        .search(search, first, max);
            } else {
                users = keycloak.realm(securityConfig.getTargetRealm())
                        .users()
                        .list(first, max);
            }

            List<Map<String, Object>> userList = users.stream()
                    .map(user -> {
                       Map<String, Object> userMap = new HashMap<>();
                       userMap.put("id", user.getId());
                       userMap.put("username", user.getUsername());
                       userMap.put("email", user.getEmail());
                       userMap.put("firstName", user.getFirstName());
                       userMap.put("lastName", user.getLastName());
                       userMap.put("enabled", user.isEnabled());
                       return userMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok().body(userList);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonList(
                            Map.of("error", "Error: " + e.getMessage())
                    ));
        }
    }

    @PostMapping("/adicionar-cpf-local/{username}/{cpf}")
    public ResponseEntity<Map<String, Object>> adicionarCpfLocal(
            @PathVariable String username,
            @PathVariable String cpf) {


        try {
            UserRepresentation user = keycloak.realm(targetRealm)
                    .users()
                    .search(username)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

            log.info("Usuário encontrado: {} (ID: {})", user.getUsername(), user.getId());

            UserResource userResource = keycloak.realm(targetRealm)
                    .users()
                    .get(user.getId());

            UserRepresentation userAtual = userResource.toRepresentation();

            Map<String, List<String>> attributes = userAtual.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            attributes.forEach((key, value) -> log.info("  - {}: {}", key, value));

            attributes.put("cpf_custom", Arrays.asList(cpf));

            userAtual.setAttributes(attributes);

            userResource.update(userAtual);

            UserRepresentation userVerificado = userResource.toRepresentation();

            boolean sucesso = userVerificado.getAttributes() != null
                    && userVerificado.getAttributes().containsKey("cpf_custom")
                    && !userVerificado.getAttributes().get("cpf_custom").isEmpty();

            Map<String, Object> response = new HashMap<>();

            if (sucesso) {
                String cpfSalvo = userVerificado.getAttributes().get("cpf_custom").get(0);

                response.put("status", "SUCESSO");
                response.put("mensagem", "CPF adicionado como atributo local");
                response.put("cpf", cpf);
                response.put("observacao", "Este CPF está salvo no Keycloak, não no LDAP");
                response.put("user_id", user.getId());
                response.put("username", user.getUsername());
            } else {
                response.put("status", "ERRO");
                response.put("mensagem", "Não foi possível adicionar CPF");
                response.put("atributos_apos_update", userVerificado.getAttributes());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERRO");
            errorResponse.put("mensagem", e.getMessage());
            errorResponse.put("tipo_erro", e.getClass().getSimpleName());

            if (e.getMessage() != null && e.getMessage().contains("read-only")) {
                errorResponse.put("sugestao", "Usuário LDAP não permite modificação direta. Use a solução com LDAP Mapper.");
            }

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

}
