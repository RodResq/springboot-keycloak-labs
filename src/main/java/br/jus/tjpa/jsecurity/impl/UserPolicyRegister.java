package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractUserPolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserPolicyRegister implements JSecurityRegister {

	@Autowired

	private SecurityService securityService;

	/**
	 * Lista com todos os User Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractUserPolicyConfiguration> userPolicies;

	@Override
	public void register() {
		if (Objects.nonNull(userPolicies)) {
			log.info("-- User Policies --");
			for (AbstractUserPolicyConfiguration policy : userPolicies) {
				UserPolicyRepresentation representation = new UserPolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
