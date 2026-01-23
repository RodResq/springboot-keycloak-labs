package br.jus.tjpa.jsecurity.service;

import br.jus.tjpa.jsecurity.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserAttributeExtractorService {

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private SecurityProperties securityProperties;


    public void extractCpfFomAllUses() {
        log.info("Inciando a extração de cpf de todos os users...");

        keycloak.realm(securityProperties.getRealm())
                .users()
                .list()
                .stream()
                .forEach(user -> extractAndSetCPF(user.getUsername()));

        log.info("Extração de cpf concluída!");
    }

    public void extractAndSetCPF(String username) {
        log.info("Extrando cpf do user: {}", username);

        UserRepresentation user = keycloak.realm(securityProperties.getRealm())
                .users()
                .search(username)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + username));

        extractAndSetCpf(user);
    }

    private boolean extractAndSetCpf(UserRepresentation user) {
        try {
            Map<String, List<String>> attributes = user.getAttributes();

            if (attributes == null) {
                attributes = new HashMap<>();
            }

            if (attributes.containsKey("cpf") && attributes.get("cpf") != null && !attributes.get("cpf").isEmpty()) {
                String cpfExistente = attributes.get("cpf").get(0);
                log.debug("Usuário {} já possui CPF: {}", user.getUsername(), cpfExistente);
                return true;
            }

            String description = null;

            if (attributes.containsKey("description")
                    && attributes.get("description") != null
                    && !attributes.get("description").isEmpty()) {
                description = attributes.get("description").get(0);
            }

            if (description == null || description.isEmpty()) {
                log.warn("Usuário {} não possui description", user.getUsername());
                return false;
            }

            String cpf = extractCpf(description);

            if (cpf != null) {
                attributes.put("cpf", List.of(cpf));
                log.info("Cpf extraido para {}: {}", user.getUsername(), cpf);
                user.setAttributes(attributes);
                keycloak.realm(securityProperties.getRealm())
                        .users()
                        .get(user.getId())
                        .update(user);

                return true;
            } else {
                log.warn("Não foi possível extrair CPF da description do usuário {}", user.getUsername());
                return false;
            }
        } catch (Exception e) {
            log.error("Erro ao processar usuário {}: {}", user.getUsername(), e.getMessage(), e);
            return false;
        }

    }

    private String extractCpf(String description) {
        Pattern pattern = Pattern.compile("CPF:([\\d\\.\\-]+)");
        Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            String cpfFormatade = matcher.group(1);
            String cpfOnyNumbers = cpfFormatade.replaceAll("\\D", "");

            if (cpfOnyNumbers.length() == 11) {
                return matcher.group(1).replaceAll("\\D", "");
            } else {
                log.warn("Cpf {} possui tamanho inválido: {}", cpfOnyNumbers, cpfOnyNumbers.length());
                return null;
            }
        }

        return null;
    }
}
