package br.jus.tjpa.jsecurity.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Protocol Mapper para incluir o CPF no token JWT
 */
@Component
@Slf4j
public class CpfProtocolMapperConfiguration extends AbstractProtocolMapperConfiguration {


    @PostConstruct
    public void init() {
        log.info("╔════════════════════════════════════════╗");
        log.info("║  CpfProtocolMapperConfiguration ATIVO  ║");
        log.info("╚════════════════════════════════════════╝");
    }

    @Override
    public void configure(ProtocolMapperRepresentation representation) {
        representation.setName("cpf");
        representation.setProtocol("openid-connect");
        representation.setProtocolMapper("oidc-usermodel-attribute-mapper");

        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", "cpf_custom");
        config.put("claim.name", "cpf");
        config.put("jsonType.label", "String");


        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("userinfo.token.claim", "true");

        config.put("multivalued", "false");
        config.put("aggregate.attrs", "false");

        representation.setConfig(config);

        log.info("╔════════════════════════════════════════╗");
        log.info("║   MAPPER CPF CONFIGURADO COM SUCESSO   ║");
        log.info("╚════════════════════════════════════════╝");
        log.info("Configurações aplicadas:");
        config.forEach((key, value) -> log.info("  {} = {}", key, value));
    }
}
