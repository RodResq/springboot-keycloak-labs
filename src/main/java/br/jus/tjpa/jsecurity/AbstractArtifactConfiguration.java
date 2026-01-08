package br.jus.tjpa.jsecurity;

import org.springframework.beans.factory.annotation.Autowired;

import br.jus.tjpa.jsecurity.config.SecurityProperties;
import lombok.Getter;

/**
 * Classe genérica de um atefato do keycloak.
 * 
 * @author jcruz
 *
 * @param <T>
 */
@Getter
public abstract class AbstractArtifactConfiguration<T> {

	@Autowired
	private SecurityProperties kcProperties;

	public abstract void configure(T representation);

}
