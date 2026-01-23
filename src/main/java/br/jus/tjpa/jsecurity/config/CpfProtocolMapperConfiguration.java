package br.jus.tjpa.jsecurity.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CpfProtocolMapperConfiguration extends AbstractProtocolMapperConfiguration {

    @Override
    public void configure(ProtocolMapperRepresentation representation) {
        representation.setName("cpf");
        representation.setProtocol("openid-connect");
        representation.setProtocolMapper("oidc-usermodel-attribute-mapper");

        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", "cpf");
        config.put("claim.name", "cpf");
        config.put("jsonType.label", "String");
        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        config.put("aggregate.attrs", "false");
        config.put("multivalued", "false");

        representation.setConfig(config);

        log.info("Protocol Mapper CPF configurado com: {}", config);
    }





}
