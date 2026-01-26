package br.jus.tjpa.jsecurity.controller;

import br.jus.tjpa.jsecurity.config.SecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/diagnostico")
public class DiagnosticoController {

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private SecurityConfig securityConfig;

    @GetMapping("/usuario/{username}/atributos")
    private ResponseEntity<Map<String, Object>> verficarAtributos(@PathVariable String username) {
        try {
            log.info("=== DIAGNÓSTICO DE ATRIBUTOS ===");
            log.info("Buscando usuário: {}", username);

            UserRepresentation user = keycloak.realm(securityConfig.getTargetRealm())
                    .users()
                    .search(username)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));

            Map<String, Object> diagnostico = new HashMap<>();
            diagnostico.put("username", user.getUsername());
            diagnostico.put("id", user.getId());
            diagnostico.put("email", user.getEmail());

            Map<String, List<String>> attributes = user.getAttributes();

            if (attributes == null || attributes.isEmpty()) {
                log.warn("Usuário NÃO possui atributos!");
                diagnostico.put("status", "SEM_ATRIBUTOS");
                diagnostico.put("atributos", null);
                diagnostico.put("tem_cpf", false);
                diagnostico.put("tem_description", false);
            } else {
                log.info("Attributos encontrados:");
                attributes.forEach((key, value) -> {
                    log.info(" - {}:{}", key, value);
                });

                diagnostico.put("attributes", attributes);
                diagnostico.put("total_atributes", attributes.size());

                boolean temCpf = attributes.containsKey("cpf") &&
                        attributes.get("cpf") != null &&
                        !attributes.get("cpf").isEmpty();

                diagnostico.put("tem_cpf", temCpf);

                if (temCpf) {
                    String cpf = attributes.get("cpf").get(0);
                    diagnostico.put("cpf_valor", cpf);
                    log.info("CPF encontrado: {}", cpf);
                } else {
                    diagnostico.put("cpf_valor", null);
                    log.warn("CPF NÃO encontrado!");
                }

                boolean temDescription = attributes.containsKey("description") &&
                        attributes.get("description") != null &&
                        !attributes.get("description").isEmpty();

                diagnostico.put("tem_description", temDescription);

                if (temDescription) {
                    String description = attributes.get("description").get(0);
                    diagnostico.put("description_valor", description);
                    log.info("Description encontrado: {}", description);
                } else {
                    diagnostico.put("description_valor", null);
                    log.warn("Description NÃO encontrado!");
                }

                if (temCpf) {
                    diagnostico.put("status", "OK");
                } else if (temDescription) {
                    diagnostico.put("status", "PRECISA_EXTRAIR_CPF");
                    diagnostico.put("mensagem", "Description existe, mas CPF precisa ser extraído");
                } else {
                    diagnostico.put("status", "SEM_DADOS");
                    diagnostico.put("mensagem", "Nem CPF nem description encontrados");
                }
            }

            log.info("Status: {}", diagnostico.get("status"));
            return ResponseEntity.ok(diagnostico);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
