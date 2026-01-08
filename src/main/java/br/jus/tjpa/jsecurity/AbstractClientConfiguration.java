package br.jus.tjpa.jsecurity;

import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;

import lombok.Getter;

/**
 * Classe de representação de um CLIENT.
 * 
 * @author jcruz
 *
 */
@Getter
public abstract class AbstractClientConfiguration extends AbstractArtifactConfiguration<ClientRepresentation> {

	/**
	 * Retorna as roles que serão adcionadas ao client.
	 * 
	 * @return
	 */
	public List<String> roles() {
		return null;
	}

	public ClientRepresentation frontend() {
		return null;
	}

}
