package br.jus.tre_pa.jsecurity.config;

import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
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

/**
 * Classe de configuração do Keycloak.
 * 
 * @author jcruz
 *
 */
@Configuration
public class SecurityConfig {

	@Autowired
	private SecurityProperties kcProperties;

	@Bean
	public Keycloak getKeycloak() {
		// @formatter:off
		return Keycloak.getInstance(
				"http://localhost:8080",
				"ciprej-realm",
				"user-ciprej",
				"123456",
				"user-ciprej");
		// @formatter:on
	}

	/**
	 * Retorna o contexto de segurança do Keycloak.
	 * 
	 * @return
	 */
	@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public Keycloak accessToken() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return (Keycloak) request.getAttribute(Keycloak.class.getName());
	}
}
