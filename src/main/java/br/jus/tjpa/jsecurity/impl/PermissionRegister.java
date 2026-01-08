package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractPermissionConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PermissionRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;
	/**
	 * Lista com todas as Permissions.
	 */
	@Autowired(required = false)
	private Collection<AbstractPermissionConfiguration> permissions;

	@Override
	public void register() {
		log.info("-- Permissions --");
		if (Objects.nonNull(permissions)) {
			for (AbstractPermissionConfiguration permission : permissions) {
				ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();
				permission.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
