package br.jus.tjpa.jsecurity.register;

import br.jus.tjpa.jsecurity.impl.*;
import jakarta.ws.rs.ProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.config.SecurityProperties;
import br.jus.tjpa.jsecurity.exception.JSecurityException;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class JSecurityAutoRegister {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private SecurityProperties securityProperties;

	@EventListener(ContextRefreshedEvent.class)
	protected void init() {
		log.info("Iniciando o auto-registro dos artefatos no Keycloak...");
		try {
			applicationContext.getBean(RealmRegister.class).register();
			applicationContext.getBean(ClientRegister.class).register();
			applicationContext.getBean(UserRegister.class).register();
			applicationContext.getBean(ProtocolMapperRegister.class);
			applicationContext.getBean(ResourceRegister.class).register();
			applicationContext.getBean(RolePolicyRegister.class).register();
			applicationContext.getBean(ClientPolicyRegister.class).register();
			applicationContext.getBean(RulePolicyRegister.class).register();
			applicationContext.getBean(JSPolicyRegister.class).register();
			applicationContext.getBean(GroupPolicyRegister.class).register();
			applicationContext.getBean(TimePolicyRegister.class).register();
			applicationContext.getBean(UserPolicyRegister.class).register();
			applicationContext.getBean(AggregatePolicyRegister.class).register();
			applicationContext.getBean(PermissionRegister.class).register();
		} catch (ProcessingException e) {
			throw new JSecurityException(String.format("Erro ao conectar ao Keycloak em: %s", securityProperties.getAuthServerUrl()));
		}
	}
}
