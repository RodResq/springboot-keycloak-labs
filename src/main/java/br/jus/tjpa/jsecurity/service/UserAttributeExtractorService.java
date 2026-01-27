package br.jus.tjpa.jsecurity.service;

import br.jus.tjpa.jsecurity.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${keycloak.target-realm}")
    private String targetRealm;


    public void extractCpfFomAllUses() {
        log.info("Inciando a extração de cpf de todos os users...");

        keycloak.realm(targetRealm)
                .users()
                .list()
                .stream()
                .forEach(user -> extractAndSetCPF(user.getUsername()));

        log.info("Extração de cpf concluída!");
    }

    public boolean extractAndSetCPF(String username) {
        log.info("Extrando cpf do user: {}", username);

        UserRepresentation user = keycloak.realm(targetRealm)
                .users()
                .search(username)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + username));

       return extractCpfFromDescription(user);
    }

    private boolean extractCpfFromDescription(UserRepresentation user) {
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

            String cpf = extractCpfFromDescription(description);

            if (cpf != null) {
                log.info("╔════════════════════════════════════════╗");
                log.info("║      SALVANDO CPF NO KEYCLOAK          ║");
                log.info("╚════════════════════════════════════════╝");
                log.info("Usuário: {}", user.getUsername());
                log.info("CPF extraído: {}", cpf);
                log.info("Description: {}", description);

                // Adicionar CPF aos atributos
                attributes.put("cpf", Arrays.asList(cpf));
                user.setAttributes(attributes);

                log.info("[1/2] Atualizando usuário no Keycloak...");

                keycloak.realm(targetRealm)
                        .users()
                        .get(user.getId())
                        .update(user);
                log.info("[2/2] Verificando se foi salvo...");

                // Verificar se foi salvo
                UserRepresentation userVerificado = keycloak.realm(targetRealm)
                        .users()
                        .get(user.getId())
                        .toRepresentation();

                if (userVerificado.getAttributes() != null
                        && userVerificado.getAttributes().containsKey("cpf")
                        && !userVerificado.getAttributes().get("cpf").isEmpty()) {
                    String cpfSalvo = userVerificado.getAttributes().get("cpf").get(0);
                    log.info("✓✓✓ CONFIRMADO: CPF salvo com sucesso: {}", cpfSalvo);
                    return true;
                } else {
                    log.error("❌ ERRO: CPF não foi salvo no Keycloak!");
                    log.error("Atributos após update: {}", userVerificado.getAttributes());
                    return false;
                }
            } else {
                log.warn("Não foi possível extrair CPF da description do usuário {}", user.getUsername());
                log.warn("Description: {}", description);
                return false;
            }
        } catch (Exception e) {
            log.error("Erro ao processar usuário {}: {}", user.getUsername(), e.getMessage(), e);
            return false;
        }

    }

    private String extractCpfFromDescription(String description) {
        Pattern pattern = Pattern.compile("CPF:([\\d\\.\\-]+)");
        Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            String cpfFormatado = matcher.group(1);
            String cpfApenasNumeros = cpfFormatado.replaceAll("\\D", "");

            if (cpfApenasNumeros.length() == 11) {
                log.info("✓ CPF extraído: {} → {}", cpfFormatado, cpfApenasNumeros);
                return cpfApenasNumeros;
            } else {
                log.warn("Cpf {} possui tamanho inválido: {}", cpfApenasNumeros, cpfApenasNumeros.length());
                return null;
            }
        }

        log.warn("⚠ Padrão 'CPF:XXX.XXX.XXX-XX' não encontrado na description");
        return null;
    }
}
