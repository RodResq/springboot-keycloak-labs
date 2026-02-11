package br.jus.tjpa.jsecurity;

import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;

import lombok.Getter;

@Getter
public abstract class AbstractClientConfiguration extends AbstractArtifactConfiguration<ClientRepresentation> {

	public List<String> roles() {
		return null;
	}

	public ClientRepresentation frontend() {
		return null;
	}

}
