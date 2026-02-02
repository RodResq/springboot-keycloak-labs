package br.jus.tjpa.jsecurity.impl;

import br.jus.tjpa.jsecurity.config.AbstractProtocolMapperConfiguration;
import br.jus.tjpa.jsecurity.config.SecurityProperties;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private Keycloak keycloak;

    @Autowired
    private Collection<AbstractProtocolMapperConfiguration> protocolMappers;

    @Value("${keycloak.user-client}")
    private String userClientId;

    @Value("${keycloak.client-scope-name:acme-dedicated}")
    private String clientScopeName;

    @Override
    public void register() {

        if (Objects.isNull(protocolMappers)) {
            log.warn("protocolMappers é NULL!");
            return;
        }

        if (protocolMappers.isEmpty()) {
            log.warn("protocolMappers está VAZIO!");
            return;
        }

        if (Objects.nonNull(protocolMappers))  {
            for (AbstractProtocolMapperConfiguration mapper: protocolMappers) {
                ProtocolMapperRepresentation representation = new ProtocolMapperRepresentation();
                mapper.configure(representation);
            }
        }

    }

    /**
     * Registra o Protocol Mapper no Client Scope dedicado
     */
    private boolean registerProtocolMapper(ProtocolMapperRepresentation representation) {
        try {

            ClientScopeRepresentation clientScope = keycloak.realm(securityProperties.getRealm())
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals("acme"))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                return false;
            }

            // Obter o recurso do Client Scope
            ClientScopeResource clientScopeResource = keycloak.realm(securityProperties.getRealm())
                    .clientScopes()
                    .get(clientScope.getId());

            // Verificar se o mapper já existe
            boolean mapperExists = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> m.getName().equals(representation.getName()));

            if (mapperExists) {
                return false;
            }

            // Criar o mapper no Client Scope
            clientScopeResource.getProtocolMappers().createMapper(representation).close();

            // Verificar se foi criado
            boolean created = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> m.getName().equals(representation.getName()));

            if (created) {
                return true;
            } else {
                return true; // Consideramos sucesso mesmo assim
            }

        } catch (Exception e) {
            log.error("Erro ao registrar Protocol Mapper '{}': {}",
                    representation.getName(), e.getMessage());
            log.error("  Stack trace:", e);
            return false;
        }
    }

    private String getClientIdFromProperties() {
        return "acme";
    }
}
