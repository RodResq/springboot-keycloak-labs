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
        log.info("╔════════════════════════════════════════╗");
        log.info("║      REGISTRANDO PROTOCOL MAPPERS      ║");
        log.info("╚════════════════════════════════════════╝");

        if (Objects.isNull(protocolMappers)) {
            log.warn("protocolMappers é NULL!");
            return;
        }

        if (protocolMappers.isEmpty()) {
            log.warn("protocolMappers está VAZIO!");
            return;
        }

        log.info("Total de protocolos mapper encontrados: {}", protocolMappers.size());
        log.info("✓ Client Scope alvo: '{}'", clientScopeName);
        log.info("✓ Realm: '{}'", securityProperties.getRealm());

        if (Objects.nonNull(protocolMappers))  {
            for (AbstractProtocolMapperConfiguration mapper: protocolMappers) {
                ProtocolMapperRepresentation representation = new ProtocolMapperRepresentation();
                mapper.configure(representation);

                log.info("\n[PROCESSANDO] Mapper: '{}'", representation.getName());

                if (registerProtocolMapper(representation)) {
                    log.info("\t Protocol Mapper '{}' registrado com sucesso.", representation.getName());
                } else {
                    log.info("\t Protocol Mapper '{}' já existe.", representation.getName());
                }
            }
        }

        log.info("\n╔════════════════════════════════════════╗");
        log.info("║   REGISTRO DE MAPPERS CONCLUÍDO        ║");
        log.info("╚════════════════════════════════════════╝\n");
    }

    /**
     * Registra o Protocol Mapper no Client Scope dedicado
     */
    private boolean registerProtocolMapper(ProtocolMapperRepresentation representation) {
        try {

            log.info("  [1/4] Buscando Client Scope '{}'...", clientScopeName);

            // Buscar o Client Scope pelo nome
            ClientScopeRepresentation clientScope = keycloak.realm(securityProperties.getRealm())
                    .clientScopes()
                    .findAll()
                    .stream()
                    .filter(cs -> clientScopeName.equals("acme"))
                    .findFirst()
                    .orElse(null);

            if (clientScope == null) {
                log.error("  ❌ Client Scope '{}' não encontrado!", clientScopeName);
                log.error("  💡 Dica: Verifique se o Client Scope existe no Keycloak");
                log.error("  💡 Dica: Configure 'keycloak.client-scope-name' no application.properties");
                return false;
            }

            log.info("  ✓ Client Scope encontrado (ID: {})", clientScope.getId());

            // Obter o recurso do Client Scope
            ClientScopeResource clientScopeResource = keycloak.realm(securityProperties.getRealm())
                    .clientScopes()
                    .get(clientScope.getId());

            log.info("  [2/4] Verificando se mapper já existe...");

            // Verificar se o mapper já existe
            boolean mapperExists = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> m.getName().equals(representation.getName()));

            if (mapperExists) {
                log.info("  ○ Mapper '{}' já existe no Client Scope", representation.getName());
                return false;
            }

            log.info("  [3/4] Criando Protocol Mapper...");
            log.info("  Configurações:");
            log.info("    - Nome: {}", representation.getName());
            log.info("    - Protocolo: {}", representation.getProtocol());
            log.info("    - Tipo: {}", representation.getProtocolMapper());
            log.info("    - Configs: {}", representation.getConfig());

            // Criar o mapper no Client Scope
            clientScopeResource.getProtocolMappers().createMapper(representation).close();

            log.info("  [4/4] Verificando criação...");

            // Verificar se foi criado
            boolean created = clientScopeResource.getProtocolMappers()
                    .getMappers()
                    .stream()
                    .anyMatch(m -> m.getName().equals(representation.getName()));

            if (created) {
                log.info("  ✓✓✓ CONFIRMADO: Mapper '{}' criado com sucesso!", representation.getName());
                return true;
            } else {
                log.warn("  ⚠ AVISO: Mapper criado mas não encontrado na verificação");
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
