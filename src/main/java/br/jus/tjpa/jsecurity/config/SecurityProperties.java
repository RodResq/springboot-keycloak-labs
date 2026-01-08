package br.jus.tjpa.jsecurity.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Classe de Properties com as propriedades do keycloak. Além das propriedades padrões do keycloak (iniciadas com o prefixo 'keycloak.') é definido o prefixo 'kc' para propriedades
 * customizadas.
 * 
 * @author jcruz
 *
 */
@Configuration
@ConfigurationProperties(prefix = "kc")
@Getter
@Setter
public class SecurityProperties {

	/**
	 * URL de conexão do keycloak.
	 */
	private String authServerUrl;

	/**
	 * Realm padrão da aplicação.
	 */
	private String realm;

	/**
	 * Secret da aplicação backend.
	 */
	private String secret;

	/**
	 * ClientId da aplicação backend.
	 */
	private String clientId;

	/**
	 * URL base da aplicação.
	 */
	private String baseUrl;

	/**
	 * URIs de redirecionamento da aplicação frontend.
	 */
	private List<String> redirectUris;

	/**
	 * Usuário administrador do realm master do Keycloak.
	 */
	private String admUser;

	/**
	 * Password do usuário administrador do realm master do Keycloak.
	 */
	private String admPass;

}
