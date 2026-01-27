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
            log.info("╔════════════════════════════════════════╗");
            log.info("║        INÍCIO DO PROCESSO LOGIN        ║");
            log.info("╚════════════════════════════════════════╝");
            log.info("Usuário: {}", loginInput.getUsername());
            log.info("Efetuando Login com o usuário: {}", loginInput.getUsername());

            log.info("\n[PASSO 1] Buscando usuário no Keycloak...");
            UserRepresentation userAntes = keycloak.realm(securityConfig.getTargetRealm())
                    .users()
                    .search(loginInput.getUsername())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

            log.info("✓ Usuário encontrado: {} (ID: {})", userAntes.getUsername(), userAntes.getId());

            log.info("\n[VERIFICAÇÃO] Atributos ANTES da extração:");
            if (userAntes.getAttributes() == null || userAntes.getAttributes().isEmpty()) {
                log.warn("Usuário NÃO possui atributos!");
            } else {
                userAntes.getAttributes().forEach((key, value) -> {
                    log.info("  - {}: {}", key, value);
                });

                if (userAntes.getAttributes().containsKey("cpf")) {
                    log.info("PF já existe: {}", userAntes.getAttributes().get("cpf"));
                } else {
                    log.warn("CPF NÃO existe ainda");
                }
            }

            log.info("\n[PASSO 2] Extraindo CPF do atributo description...");
            boolean cpfExtraido = userAttributeExtractorService.extractAndSetCPF(loginInput.getUsername());

            if (cpfExtraido) {
                log.info("CPF extraído e salvo com sucesso");
            } else {
                log.warn("Não foi possível extrair o CPF");
            }

            log.info("\n[PASSO 3] Verificando atributos APÓS extração...");
            UserRepresentation userDepois = keycloak.realm(securityConfig.getTargetRealm())
                    .users()
                    .get(userAntes.getId())
                    .toRepresentation();

            if (userDepois.getAttributes() == null || userDepois.getAttributes().isEmpty()) {
                log.error("ERRO: Usuário ainda NÃO possui atributos!");
            } else {
                log.info("Atributos atualizados:");
                userDepois.getAttributes().forEach((key, value) -> {
                    log.info("  - {}: {}", key, value);
                });

                if (userDepois.getAttributes().containsKey("cpf")) {
                    List<String> cpfValues = userDepois.getAttributes().get("cpf");
                    if (cpfValues != null && !cpfValues.isEmpty()) {
                        String cpf = cpfValues.get(0);
                        log.info(" ✓✓✓ CPF CONFIGURADO: {} ✓✓✓", cpf);
                    }
                } else {
                    log.error("CPF AINDA NÃO EXISTE!");
                    log.error("Isso significa que o token NÃO terá o CPF!");
                }
            }

            log.info("\n[PASSO 4] Realizando login no Keycloak...");
            AccessTokenResponse tokenResponse = securityService.login(
                            loginInput.getUsername(),
                            loginInput.getPassword()
            );

            log.info("Token gerado com sucesso");

            log.info("\n[PASSO 5] Analisando token JWT...");
            String[] tokenParts = tokenResponse.getToken().split("\\.");
            if (tokenParts.length > 2) {
                String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));

                log.info("Token JWT Payload:");
                log.info("{}" , payload);

                if (payload.contains("\"cpf\"")) {
                    log.info("\\n✓✓✓ SUCESSO: CPF PRESENTE NO TOKEN! ✓✓✓");

                    int cpfIndex = payload.indexOf("\\\"cpf\\\"");
                    if (cpfIndex > 0) {
                        int start = payload.indexOf(":", cpfIndex) + 1;
                        int end = payload.indexOf(",", start);

                        if (end == -1) end = payload.indexOf("}", start);
                        String cpfValue = payload.substring(start, end).trim().replace("\"", "");
                        log.info("Valor do CPF no token: {}", cpfValue);
                    }
                } else {
                    log.error("FALHA: CPF NÃO ESTÁ NO TOKEN! ❌❌❌");
                    log.error("Possíveis causas:");
                    log.error("1. O atributo 'cpf' não existe no usuário");
                    log.error("2. O Protocol Mapper não está configurado corretamente");
                    log.error("3. O Protocol Mapper está no cliente errado");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokenResponse.getToken());
            response.put("token_type", tokenResponse.getTokenType());
            response.put("expires_in", tokenResponse.getExpiresIn());
            response.put("refresh_token", tokenResponse.getRefreshToken());

            log.info("\n╔════════════════════════════════════════╗");
            log.info("║          LOGIN CONCLUÍDO               ║");
            log.info("╚════════════════════════════════════════╝\n");


            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("\n╔════════════════════════════════════════╗");
            log.error("║          ERRO NO LOGIN                 ║");
            log.error("╚════════════════════════════════════════╝");
            log.error("Erro: {}", e.getMessage(), e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


    @PostMapping("/usuarios/extrair-cpf")
    public ResponseEntity<Map<String, Object>> extrairCpfTodosUsuarios() {
        try {
            log.info("╔════════════════════════════════════════╗");
            log.info("║   EXTRAÇÃO DE CPF - TODOS USUÁRIOS     ║");
            log.info("╚════════════════════════════════════════╝");

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

        log.info("╔════════════════════════════════════════╗");
        log.info("║   ADICIONANDO CPF LOCAL (WORKAROUND)   ║");
        log.info("╚════════════════════════════════════════╝");
        log.info("Usuário: {}", username);
        log.info("CPF: {}", cpf);

        try {
            // 1. Buscar usuário
            log.info("[1/4] Buscando usuário...");
            UserRepresentation user = keycloak.realm(targetRealm)
                    .users()
                    .search(username)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

            log.info("✓ Usuário encontrado: {} (ID: {})", user.getUsername(), user.getId());

            // 2. Obter UserResource
            log.info("[2/4] Obtendo recurso do usuário...");
            UserResource userResource = keycloak.realm(targetRealm)
                    .users()
                    .get(user.getId());

            // 3. Obter representação ATUAL com todos os atributos
            log.info("[3/4] Obtendo atributos atuais...");
            UserRepresentation userAtual = userResource.toRepresentation();

            Map<String, List<String>> attributes = userAtual.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            log.info("Atributos ANTES:");
            attributes.forEach((key, value) -> log.info("  - {}: {}", key, value));

            log.info("[4/4] Adicionando cpf_custom...");
            attributes.put("cpf_custom", Arrays.asList(cpf));

            // IMPORTANTE: Usar a representação ATUAL, não a antiga
            userAtual.setAttributes(attributes);

            // 5. Atualizar no Keycloak
            log.info("Atualizando usuário no Keycloak...");
            userResource.update(userAtual);

            // 6. Verificar se foi salvo
            log.info("Verificando se foi salvo...");
            UserRepresentation userVerificado = userResource.toRepresentation();

            log.info("Atributos DEPOIS:");
            boolean sucesso = userVerificado.getAttributes() != null
                    && userVerificado.getAttributes().containsKey("cpf_custom")
                    && !userVerificado.getAttributes().get("cpf_custom").isEmpty();

            Map<String, Object> response = new HashMap<>();

            if (sucesso) {
                String cpfSalvo = userVerificado.getAttributes().get("cpf_custom").get(0);
                log.info("✓✓✓ SUCESSO: CPF salvo: {}", cpfSalvo);

                response.put("status", "SUCESSO");
                response.put("mensagem", "CPF adicionado como atributo local");
                response.put("cpf", cpf);
                response.put("observacao", "Este CPF está salvo no Keycloak, não no LDAP");
                response.put("user_id", user.getId());
                response.put("username", user.getUsername());
            } else {
                log.error("❌ ERRO: CPF não foi salvo");
                log.error("Atributos após update: {}", userVerificado.getAttributes());

                response.put("status", "ERRO");
                response.put("mensagem", "Não foi possível adicionar CPF");
                response.put("atributos_apos_update", userVerificado.getAttributes());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ ERRO ao adicionar CPF: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERRO");
            errorResponse.put("mensagem", e.getMessage());
            errorResponse.put("tipo_erro", e.getClass().getSimpleName());

            // Se for erro de permissão ou readonly
            if (e.getMessage() != null && e.getMessage().contains("read-only")) {
                errorResponse.put("sugestao", "Usuário LDAP não permite modificação direta. Use a solução com LDAP Mapper.");
            }

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

}
