package br.jus.tre_pa.jsecurity.controller;

import br.jus.tre_pa.jsecurity.config.SecurityConfig;
import org.keycloak.admin.client.Keycloak;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
