package br.jus.tjpa.jsecurity.impl;

import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import br.jus.tjpa.jsecurity.config.SecurityProperties;
import br.jus.tjpa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityServiceImpl implements SecurityService {

	@Autowired
	private Keycloak keycloak;

	@Autowired
	private SecurityProperties kcProperties;

	@Value("${keycloak.auth-server-url}")
	private String serverUrl;

	@Value("${keycloak.realm}")
	private String realm;

	@Override
	public boolean register(RealmRepresentation representation) {
		if (!hasRealm()) {
			Assert.hasText(representation.getRealm(), "O atributo 'realm' do realm deve ser definido.");
			keycloak.realms().create(representation);
			log.info("\t Realm '{}' criado com sucesso.", kcProperties.getRealm());
			return true;
		}
		log.info("\t Realm '{}' já existe.", kcProperties.getRealm());
		return false;
	}

	@Override
	public boolean register(ClientRepresentation representation) {
		Assert.hasText(representation.getClientId(), String.format("O atributo 'clientId' do client (%s) deve ser definido.", representation.getClass().getName()));
		if (!hasClient(representation.getClientId())) {
			keycloak.realm(kcProperties.getRealm()).clients().create(representation).close();
			// Verifica se o recurso de authorization está habilitado para o client.
			if (Objects.nonNull(representation.getAuthorizationServicesEnabled())) {
				// Remove o resource default gerado com o client. @formatter:off
				getClientResource().authorization().resources().resources().stream()
					.filter(p -> p.getName().equals("Default Resource"))
					.findAny()
					.ifPresent(p -> getClientResource().authorization().resources().resource(p.getId()).remove());
				// @formatter:on
				// Remove a policy default gerada com o client. @formatter:off
				getClientResource().authorization().policies().policies().stream()
					.filter(p -> p.getName().equals("Default Policy"))
					.findAny()
					.ifPresent(p -> getClientResource().authorization().policies().policy(p.getId()).remove());
				// @formatter:on
			}
			log.info("\t Client '{}' registrado com sucesso.", representation.getClientId());
			return true;
		}
		log.info("\t Client '{}' já existe.", representation.getClientId());
		return false;
	}

	// TODO Verificar a existência dos Scopes antes de criar o Resource.
	@Override
	public boolean register(ResourceRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' do resource deve ser definido.");
		Assert.notEmpty(representation.getScopes(), "O atributo 'scopes' do resource deve ser definido.");

		if (!hasResource(representation.getName())) {
			getClientResource().authorization().resources().create(representation).close();
			log.info("\t Resource '{}' registrado com sucesso.", representation.getName());
			return true;
		}
		log.info("\t Resource '{}' já existe.", representation.getName());
		return false;
	}

	// TODO Exibir nome da classe no erro de assert.
	@Override
	public boolean register(ClientPolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' da ClientPolicy '{}' é obrigatório.");
		Assert.notEmpty(representation.getClients(), "É necessário adicionar pelo menos 1 client a policy.");
		// Verifica se o policy existe
		if (!hasClient(representation.getName())) {
			getClientResource().authorization().policies().client().create(representation).close();
			log.info("\t Client Policy '{}' registrado com sucesso.", representation.getName());
			return true;
		}
		log.info("\t Client Policy '{}' já existe.");
		return false;
	}

	@Override
	public boolean register(RolePolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' da RolePolicy é obrigatório.");
		// Verifica se o role existe ou no client ou no realm.
		if (hasClientRole(representation.getName()) || hasRealmRole(representation.getName())) {
			getClientResource().authorization().policies().role().create(representation).close();
			log.info("\t Role Policy '{}' registrada com sucesso.", representation.getName());
			return true;
		}
		log.info("Role '{}' inexistente. ", representation.getName());
		return false;
	}

	@Override
	public boolean register(GroupPolicyRepresentation representation) {
		Assert.hasLength(representation.getName(), "O atributo 'name' da GrupoPolicy é obrigatório.");
		Assert.notEmpty(representation.getGroups(), String.format("É necessário atribuir pelo menos 1 group a GroupPolicy '%s'.", representation.getName()));

		getClientResource().authorization().policies().group().create(representation).close();
		log.info("\t Group Policy '{}' registrado com sucesso.", representation.getName());
		return true;
	}

	@Override
	public boolean register(JSPolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' é obrigatório na JSPolicy.");
		Assert.hasText(representation.getCode(), String.format("O atributo 'code' é obrigatório na JSPolicy '%s'.", representation.getName()));

		getClientResource().authorization().policies().js().create(representation).close();
		log.info("\t Js Policy '{}' registrado com sucesso.", representation.getName());
		return true;
	}

	@Override
	public boolean register(PolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' é obrigatório na RulePolicy.");

		getClientResource().authorization().policies().create(representation).close();
		log.info("\t Rule Policy '{}' registrado com sucesso.", representation.getName());
		return true;
	}

	@Override
	public boolean register(TimePolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' é obrigatório na TimePolicy.");

		getClientResource().authorization().policies().time().create(representation).close();
		log.info("\t Time Policy '{}' registrado com sucesso.", representation.getName());
		return true;
	}

	@Override
	public boolean register(UserPolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' é obrigatório na UserPolicy.");
		getClientResource().authorization().policies().user().create(representation).close();
		log.info("\t User Policy '{}' registrado com sucesso.", representation.getName());
		return true;
	}

	@Override
	public boolean register(AggregatePolicyRepresentation representation) {
		Assert.hasText(representation.getName(), "O atributo 'name' da aggregate policy deve ser definido.");
		Assert.notEmpty(representation.getPolicies(), "o atributo 'policies' da agregate policy deve ser definido.");

		getClientResource().authorization().policies().aggregate().create(representation).close();
		log.info("Aggregate Policy '{}' registrada com sucesso.", representation.getName());
		return true;
	}

	/**
	 * Método registraro de Permission.
	 * 
	 * @param permission
	 */
	@Override
	public boolean register(ResourcePermissionRepresentation representation) {
		getClientResource().authorization().permissions().resource().create(representation).close();
		log.info("\t Permission '{}' registrado com sucesso.", representation.getName());
		return true;
	}

	/*
	 * 
	 */
	@Override
	public boolean register(UserRepresentation representation) {
		Assert.hasText(representation.getUsername(), "O atributo 'username' é obrigatório.");
		Assert.hasText(representation.getEmail(), "O atributo 'email' é obrigatório.");
		if (!hasUser(representation.getUsername())) {
			keycloak.realm(kcProperties.getRealm()).users().create(representation).close();
			log.info("\t Usuário '{}' registrado com sucesso.", representation.getUsername());
			return true;
		}
		log.info("\t Usuário '{}' já existe.", representation.getUsername());
		return false;
	}

	private ClientResource getClientResource() {
		// @formatter:off
		return  keycloak.realm(kcProperties.getRealm()).clients().findByClientId(kcProperties.getClientId()).stream()
				.map(client-> keycloak.realm(kcProperties.getRealm()).clients().get(client.getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Erro ao encontrar client '%s'", kcProperties.getClientId() )));
		// @formatter:on
	}

	@Override
	public ClientResource getClientResource(String clientId) {
		return  keycloak.realm(kcProperties.getRealm()).clients().findByClientId(clientId).stream()
				.map(client-> keycloak.realm(kcProperties.getRealm()).clients().get(client.getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Erro ao encontrar client '%s'", clientId )));
	}

	@Override
	public AccessTokenResponse login(String clientId, String username, String password) {
		try(Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.grantType(OAuth2Constants.PASSWORD)
				.clientId(clientId)
				.username(username)
				.password(password)
				.build()) {

			return keycloak.tokenManager().getAccessToken();
		}
	}

	@Override
	public AccessTokenResponse refreshToken(String clientId, String refreshToken) {
		try(Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.grantType(OAuth2Constants.REFRESH_TOKEN)
				.clientId(clientId)
				.build()
		) {

			return keycloak.tokenManager().refreshToken();
		}
	}

	private boolean hasRealm() {
		return keycloak.realms().findAll().stream().anyMatch(r -> r.getRealm().equals(kcProperties.getRealm()));
	}

	private boolean hasClient(String clientId) {
		// @formatter:off
		return  keycloak.realm(kcProperties.getRealm()).clients().findByClientId(clientId).stream()
				.map(client-> keycloak.realm(kcProperties.getRealm()).clients().get(client.getId()))
				.findFirst()
				.isPresent();
		// @formatter:on
	}

	private boolean hasUser(String username) {
		return keycloak.realm(kcProperties.getRealm()).users().search(username).isEmpty() == false;
	}

	private boolean hasScope(String scopeName) {
		return Objects.nonNull(getClientResource().authorization().scopes().findByName(scopeName));
	}

	private boolean hasPolicy(String policyName) {
		return Objects.nonNull(getClientResource().authorization().policies().findByName(policyName));
	}

	private boolean hasResource(String resourceName) {
		return getClientResource().authorization().resources().findByName(resourceName).isEmpty() == false;
	}

	private boolean hasClientRole(String roleName) {
		return Objects.nonNull(getClientResource().roles().get(roleName));
	}

	private boolean hasRealmRole(String roleName) {
		return Objects.nonNull(keycloak.realm(kcProperties.getRealm()).roles().get(roleName));
	}

}
