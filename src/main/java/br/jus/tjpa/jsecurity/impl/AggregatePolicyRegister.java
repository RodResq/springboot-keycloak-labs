package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractAggregatePolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;

@Component
public class AggregatePolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;
	/**
	 * Lista com todos os Aggregate Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractAggregatePolicyConfiguration> aggregatePolicies;

	@Override
	public void register() {
		if (Objects.nonNull(aggregatePolicies)) {
			for (AbstractAggregatePolicyConfiguration policy : aggregatePolicies) {
				AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
