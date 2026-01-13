package br.jus.tjpa.jsecurity.controller;

import br.jus.tjpa.jsecurity.config.SecurityConfig;
import br.jus.tjpa.jsecurity.model.input.LoginInput;
import br.jus.tjpa.jsecurity.service.SecurityService;
import jakarta.validation.Valid;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class MockKeyclockController {

    private SecurityConfig securityConfig;

    public MockKeyclockController(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Autowired
    private SecurityService securityService;


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
            AccessTokenResponse tokenResponse =
                    securityService.login(loginInput.getClientId(), loginInput.getUsername(), loginInput.getPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokenResponse.getToken());

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return (ResponseEntity<List<String>>) Collections.singletonList("Error: " + e.getMessage());
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

}
