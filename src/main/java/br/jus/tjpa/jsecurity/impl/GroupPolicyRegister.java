package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractGroupPolicyConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GroupPolicyRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os Group Policies.
	 */
	@Autowired(required = false)
	private Collection<AbstractGroupPolicyConfiguration> groupPolicies;

	@Override
	public void register() {
		if (Objects.nonNull(groupPolicies)) {
			log.info("-- Group Polcies --");
			for (AbstractGroupPolicyConfiguration policy : groupPolicies) {
				GroupPolicyRepresentation representation = new GroupPolicyRepresentation();
				policy.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
