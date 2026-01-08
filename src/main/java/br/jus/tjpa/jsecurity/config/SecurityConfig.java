package br.jus.tjpa.jsecurity.config;

import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Configuration
public class SecurityConfig {

	@Value("${keycloak.auth-server-url}")
	private String serverUrl;

	@Value("${keycloak.realm}")
	private String realm;

	@Value("${keycloak.username}")
	private String userName;

	@Value(("${keycloak.password}"))
	private String password;

	@Value(("${keycloak.client}"))
	private String client;

	@Value("${keycloak.credentials.secret}")
	private String secret;

	@Bean
	public Keycloak getKeycloak() {
		return KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.username(userName)
				.password(password)
				.clientId(client)
				.clientSecret(secret)
				.grantType(OAuth2Constants.PASSWORD)
				.build();
	}

	@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public Keycloak accessToken() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return (Keycloak) request.getAttribute(Keycloak.class.getName());
	}
}
