package br.jus.tjpa.jsecurity.impl;

import br.jus.tjpa.jsecurity.config.AbstractProtocolMapperConfiguration;
import br.jus.tjpa.jsecurity.config.SecurityProperties;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
@Slf4j
public class ProtocolMapperRegister implements JSecurityRegister {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private Collection<AbstractProtocolMapperConfiguration> protocolMappers;

    @Override
    public void register() {
        log.info("-- Prototocol Mappers --");

        if (Objects.isNull(protocolMappers)) {
            log.warn("protocolMappers é NULL!");
            return;
        }

        if (protocolMappers.isEmpty()) {
            log.warn("protocolMappers está VAZIO!");
            return;
        }

        log.info("Total de protocolos mapper encontrados: {}", protocolMappers.size());

        if (Objects.nonNull(protocolMappers))  {
            for (AbstractProtocolMapperConfiguration mapper: protocolMappers) {
                ProtocolMapperRepresentation representation = new ProtocolMapperRepresentation();
                mapper.configure(representation);

                if (registerProtocolMapper(representation)) {
                    log.info("\t Protocol Mapper '{}' registrado com sucesso.", representation.getName());
                } else {
                    log.info("\t Protocol Mapper '{}' já existe.", representation.getName());
                }
            }
        }
    }

    private boolean registerProtocolMapper(ProtocolMapperRepresentation representation) {
        try {
            ClientResource clientResource = securityService.getClientResource(securityProperties.getClientId());

            boolean protocolMapperExists = clientResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> m.getName().equals(representation.getName()));

            if (!protocolMapperExists) {
                clientResource.getProtocolMappers().createMapper(representation);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Erro ao registrar Protocol Mapper '{}': {}",
                    representation.getName(), e.getMessage());
            return false;
        }
    }

    private String getClientIdFromProperties() {
        return "acme";
    }
}
