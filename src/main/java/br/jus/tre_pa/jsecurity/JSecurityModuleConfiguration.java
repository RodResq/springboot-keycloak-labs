package br.jus.tre_pa.jsecurity;


import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ComponentScan(basePackages = "br.jus.tre_pa.jsecurity")
@AutoConfigurationPackage
public class JSecurityModuleConfiguration {

	@PostConstruct
	private void init() {
		log.info("-- JSecurityModuleConfiguration loaded --");
	}

}
