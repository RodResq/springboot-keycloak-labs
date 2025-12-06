package br.jus.tre_pa.jsecurity.impl;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.tre_pa.jsecurity.AbstractRealmConfiguration;
import br.jus.tre_pa.jsecurity.register.JSecurityRegister;
import br.jus.tre_pa.jsecurity.service.SecurityService;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe que registra os realms no Keycloak.
 * 
 * @author jcruz
 *
 */
@Component
@Slf4j
public class RealmRegister implements JSecurityRegister {

	@Autowired
	private SecurityService secService;

	/**
	 * Lista com todos os realms.
	 */
	@Autowired(required = false)
	private Collection<AbstractRealmConfiguration> realms;

	@Override
	public void register() {
		log.debug("-- Realm --");
		if (Objects.nonNull(realms)) {
			for (AbstractRealmConfiguration realm : realms) {
				RealmRepresentation realmRepresentation = new RealmRepresentation();
				realm.configure(realmRepresentation);
				secService.register(realmRepresentation);
			}
		}
	}
}
