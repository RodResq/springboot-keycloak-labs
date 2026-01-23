package br.jus.tjpa.jsecurity.listener;

import br.jus.tjpa.jsecurity.service.UserAttributeExtractorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserAttributeExtractionListener {

    @Autowired
    private UserAttributeExtractorService userAttributeExtractorService;

    @Value("${kc.auto-extract-attributes:false}")
    private boolean autoExtractAttributes;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (autoExtractAttributes) {
            log.info("Auto-extração de atributos habilitada. Processando usuários");
            try {
                userAttributeExtractorService.extractCpfFomAllUses();
                //TODO Atribuir o cpf ao token
                log.info("Extração automatica de atributos concluída com sucesso!");
            } catch (Exception e) {
                log.error("Erro ao extrair atributos automaticamente: {}", e.getMessage(), e);
            }
        } else {
            log.info("Auto-extração de atributos desabilitada.");
            log.info("Para habilitar, adicione: kc.auto-extract-attributes=true no application.properties");
        }
    }
}
