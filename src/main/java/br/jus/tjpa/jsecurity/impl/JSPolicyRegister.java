package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractJsPolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JSPolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os Js Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractJsPolicyConfiguration> jsPolicies;

	@Override
	public void register() {
		if (Objects.nonNull(jsPolicies)) {
			log.info("-- JS Polices --");
			for (AbstractJsPolicyConfiguration policy : jsPolicies) {
				JSPolicyRepresentation representation = new JSPolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
