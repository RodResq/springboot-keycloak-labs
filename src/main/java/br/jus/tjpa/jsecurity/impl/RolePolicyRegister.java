package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractRolePolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RolePolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os Role Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractRolePolicyConfiguration> rolePolicies;

	@Override
	public void register() {
		if (Objects.nonNull(rolePolicies)) {
			log.info("-- Role Policies --");
			for (AbstractRolePolicyConfiguration policy : rolePolicies) {
				PolicyRepresentation representation = new PolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
