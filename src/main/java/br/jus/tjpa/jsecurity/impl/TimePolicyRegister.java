package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractTimePolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TimePolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os Time Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractTimePolicyConfiguration> timePolicies;

	@Override
	public void register() {
		if (Objects.nonNull(timePolicies)) {
			log.info("-- Time Policies --");
			for (AbstractTimePolicyConfiguration policy : timePolicies) {
				TimePolicyRepresentation representation = new TimePolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
