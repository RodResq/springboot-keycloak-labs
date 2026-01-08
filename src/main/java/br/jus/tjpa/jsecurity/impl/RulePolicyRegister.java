package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractRulePolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RulePolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os Rule Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractRulePolicyConfiguration> rulePolicies;

	@Override
	public void register() {
		if (Objects.nonNull(rulePolicies)) {
			log.info("-- Rule Policies --");
			for (AbstractRulePolicyConfiguration policy : rulePolicies) {
				PolicyRepresentation representation = new PolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
