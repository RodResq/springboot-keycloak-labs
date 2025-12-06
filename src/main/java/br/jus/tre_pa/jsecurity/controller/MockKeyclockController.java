package br.jus.tre_pa.jsecurity.controller;

import br.jus.tre_pa.jsecurity.config.SecurityConfig;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/teste")
public class MockKeyclockController {

    private SecurityConfig securityConfig;

    public MockKeyclockController(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
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

}
