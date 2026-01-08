package br.jus.tjpa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tjpa.jsecurity.AbstractResourceConfiguration;
import br.jus.tjpa.jsecurity.register.JSecurityRegister;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ResourceRegister implements JSecurityRegister {

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os resources.
	 */
	@Autowired(required = false)
	private Collection<AbstractResourceConfiguration> resources;

	@Override
	public void register() {
		log.info("-- Resources --");
		if (Objects.nonNull(resources)) {
			for (AbstractResourceConfiguration resource : resources) {
				ResourceRepresentation representation = new ResourceRepresentation();
				resource.configure(representation);
				securityService.register(representation);
			}
		}
	}

}
