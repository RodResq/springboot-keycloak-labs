package br.jus.tjpa.jsecurity.config;

import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Configuration
public class SecurityConfig {

	@Autowired
	private SecurityProperties kcProperties;

	@Bean
	public Keycloak getKeycloak() {
		return KeycloakBuilder.builder()
				.serverUrl("http://localhost:8080")
				.realm("ciprej-realm")
				.username("ciprej-user")
				.password("123")
				.clientId("ciprej-client")
				.clientSecret("vCayfDqusgWsLRH8SVlRPizMpAD76Rts")
				.grantType(OAuth2Constants.PASSWORD)
				.build();
	}

	@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public Keycloak accessToken() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return (Keycloak) request.getAttribute(Keycloak.class.getName());
	}
}
