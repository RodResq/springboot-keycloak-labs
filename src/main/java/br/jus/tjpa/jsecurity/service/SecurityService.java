package br.jus.tjpa.jsecurity.service;


import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.*;

public interface SecurityService {

	boolean register(RealmRepresentation representation);

	boolean register(ClientRepresentation representation);

	boolean register(ResourceRepresentation representation);

	boolean register(ClientPolicyRepresentation representation);

	boolean register(RolePolicyRepresentation representation);

	boolean register(GroupPolicyRepresentation representation);

	boolean register(JSPolicyRepresentation representation);

	boolean register(PolicyRepresentation representation);

	boolean register(TimePolicyRepresentation representation);

	boolean register(UserPolicyRepresentation representation);

	boolean register(AggregatePolicyRepresentation representation);

	boolean register(ResourcePermissionRepresentation representation);

	boolean register(UserRepresentation representation);

	ClientResource getClientResource(String clientId);

	AccessTokenResponse login(String username, String password);

	AccessTokenResponse login(String clientId, String username, String password);

	AccessTokenResponse refreshToken(String clientId, String refreshToken);
}
