package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractClientPolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ClientPolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos Client Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractClientPolicyConfiguration> clientPolicies;

	@Override
	public void register() {
		if (Objects.nonNull(clientPolicies)) {
			log.info("-- Client Polcies --");
			for (AbstractClientPolicyConfiguration policy : clientPolicies) {
				ClientPolicyRepresentation representation = new ClientPolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}
}
