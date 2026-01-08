package br.jus.tjpa.jsecurity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;


import com.google.common.collect.Sets;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

/**
 * Classe que representa um POLICY ROLE.
 * 
 * @author jcruz
 *
 */
public abstract class AbstractRolePolicyConfiguration extends AbstractArtifactConfiguration<PolicyRepresentation> {

	// @formatter:off
	protected HashSet<Stream<PolicyRepresentation>> roles(String...roles) {
		return Sets.newHashSet(Arrays.asList(roles)
				.stream()
				.map(role -> new PolicyRepresentation()));
	}
	// @formatter:on

}
