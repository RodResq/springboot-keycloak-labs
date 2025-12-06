package br.jus.tre_pa.jsecurity.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import br.jus.tre_pa.jsecurity.AbstractClientConfiguration;
import br.jus.tre_pa.jsecurity.register.JSecurityRegister;
import br.jus.tre_pa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe que registra o client no Keycloak.
 * 
 * @author jcruz
 *
 */
@Component
@Slf4j
public class ClientRegister implements JSecurityRegister {

	@Autowired
	private Keycloak keycloak;

	@Autowired
	private SecurityService securityService;

	/**
	 * Lista com todos os clients.
	 */
	@Autowired(required = false)
	private Collection<AbstractClientConfiguration> clientsConf;

	@Override
	public void register() {
		log.info("-- Client --");
		if (Objects.nonNull(clientsConf)) {
			for (AbstractClientConfiguration clientConf : clientsConf) {
				ClientRepresentation representation = new ClientRepresentation();
				clientConf.configure(representation);
				if (securityService.register(representation)) {
					if (Objects.nonNull(clientConf.roles())) addRoles(clientConf, representation.getClientId());
					// Registra o fronted
					if (Objects.nonNull(clientConf.frontend())) {
						securityService.register(clientConf.frontend());
						setupScopeMappings(clientConf, representation);
					}
				}
			}
		}
	}

	private void setupScopeMappings(AbstractClientConfiguration clientConf, ClientRepresentation representation) {
		List<String> roles = new ArrayList<>();
		if (Objects.nonNull(representation.getDefaultRoles())) roles.addAll(Arrays.asList(representation.getDefaultRoles()));
		if (Objects.nonNull(clientConf.roles())) roles.addAll(clientConf.roles());
		ClientResource frontendClientResource = securityService.getClientResource(clientConf.frontend().getClientId());
		ClientResource backendClientResource = securityService.getClientResource(representation.getClientId());
		// @formatter:off
		frontendClientResource.getScopeMappings().clientLevel(backendClientResource.toRepresentation().getId())
			.add(roles
					.stream()
					.map(role -> new RoleRepresentation(role, role, false))
					.collect(Collectors.toList()));
		// @formatter:on
	}

	private void addRoles(AbstractClientConfiguration clientConf, String clientId) {
		// @formatter:off
		clientConf.roles().stream()
			.filter(role -> !StringUtils.isEmpty(role))
			.map(role -> new RoleRepresentation(role, role, false))
			.forEach(role -> securityService.getClientResource(clientId).roles().create(role));
		// @formatter:on
	}

}
