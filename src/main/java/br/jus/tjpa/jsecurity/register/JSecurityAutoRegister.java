package br.jus.tjpa.jsecurity.register;

import jakarta.ws.rs.ProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.config.SecurityProperties;
import br.jus.tjpa.jsecurity.exception.JSecurityException;
import br.jus.tjpa.jsecurity.impl.AggregatePolicyRegister;
import br.jus.tjpa.jsecurity.impl.ClientPolicyRegister;
import br.jus.tjpa.jsecurity.impl.ClientRegister;
import br.jus.tjpa.jsecurity.impl.GroupPolicyRegister;
import br.jus.tjpa.jsecurity.impl.JSPolicyRegister;
import br.jus.tjpa.jsecurity.impl.PermissionRegister;
import br.jus.tjpa.jsecurity.impl.RealmRegister;
import br.jus.tjpa.jsecurity.impl.ResourceRegister;
import br.jus.tjpa.jsecurity.impl.RolePolicyRegister;
import br.jus.tjpa.jsecurity.impl.RulePolicyRegister;
import br.jus.tjpa.jsecurity.impl.TimePolicyRegister;
import br.jus.tjpa.jsecurity.impl.UserPolicyRegister;
import br.jus.tjpa.jsecurity.impl.UserRegister;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe que realiza o registro automático (inicialização da aplicação) dos arrtefatos no Keycloak.
 * 
 * @author jcruz
 *
 */
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
