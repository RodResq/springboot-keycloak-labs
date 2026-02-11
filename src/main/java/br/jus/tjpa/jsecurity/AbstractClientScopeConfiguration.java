package br.jus.tjpa.jsecurity;

import lombok.Getter;
import org.keycloak.representations.idm.ClientScopeRepresentation;

@Getter
public abstract class AbstractClientScopeConfiguration extends AbstractArtifactConfiguration<ClientScopeRepresentation> {

}
