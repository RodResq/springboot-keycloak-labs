package br.jus.tjpa.jsecurity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kc")
@Getter
@Setter
public class SecurityProperties {

	private String authServerUrl;

	private String realm;

	private String secret;

	private String clientId;

	private String baseUrl;

	private List<String> redirectUris;

	private String admUser;

	private String admPass;

}
