package br.jus.tjpa.jsecurity.controller;

import br.jus.tjpa.jsecurity.config.AbstractProtocolMapperConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Controller para diagnóstico de beans do Spring
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/diagnostico")
public class BeanDiagnosticoController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private Collection<AbstractProtocolMapperConfiguration> protocolMappers;

    /**
     * Verifica todos os beans de Protocol Mapper registrados no Spring
     */
    @GetMapping("/protocol-mappers")
    public ResponseEntity<Map<String, Object>> verificarProtocolMappers() {
        log.info("=== DIAGNÓSTICO DE PROTOCOL MAPPERS ===");

        Map<String, Object> resultado = new HashMap<>();

        // Verificar injeção direta
        if (protocolMappers == null) {
            log.error("❌ protocolMappers injetado é NULL");
            resultado.put("injecao_direta", "NULL");
        } else if (protocolMappers.isEmpty()) {
            log.error("❌ protocolMappers injetado está VAZIO");
            resultado.put("injecao_direta", "VAZIO");
            resultado.put("tamanho", 0);
        } else {
            log.info("✓ protocolMappers injetado: {} beans", protocolMappers.size());
            resultado.put("injecao_direta", "OK");
            resultado.put("tamanho", protocolMappers.size());

            List<String> mapperNames = new ArrayList<>();
            protocolMappers.forEach(mapper -> {
                String className = mapper.getClass().getName();
                mapperNames.add(className);
                log.info("  - {}", className);
            });
            resultado.put("mappers_injetados", mapperNames);
        }

        // Buscar beans via ApplicationContext
        log.info("\nBuscando beans via ApplicationContext...");
        Map<String, AbstractProtocolMapperConfiguration> beansFound =
                applicationContext.getBeansOfType(AbstractProtocolMapperConfiguration.class);

        if (beansFound.isEmpty()) {
            log.error("❌ Nenhum bean de AbstractProtocolMapperConfiguration encontrado via ApplicationContext");
            resultado.put("beans_no_contexto", "NENHUM");
        } else {
            log.info("✓ Beans encontrados via ApplicationContext: {}", beansFound.size());
            resultado.put("beans_no_contexto", beansFound.size());

            List<Map<String, String>> beansList = new ArrayList<>();
            beansFound.forEach((beanName, bean) -> {
                log.info("  Bean: {} → Classe: {}", beanName, bean.getClass().getName());
                Map<String, String> beanInfo = new HashMap<>();
                beanInfo.put("nome", beanName);
                beanInfo.put("classe", bean.getClass().getName());
                beansList.add(beanInfo);
            });
            resultado.put("beans_detalhes", beansList);
        }

        // Verificar se CpfProtocolMapperConfiguration existe
        boolean cpfMapperExists = applicationContext.containsBean("cpfProtocolMapperConfiguration");
        log.info("\nBean 'cpfProtocolMapperConfiguration' existe? {}", cpfMapperExists);
        resultado.put("cpf_mapper_existe", cpfMapperExists);

        if (cpfMapperExists) {
            try {
                Object bean = applicationContext.getBean("cpfProtocolMapperConfiguration");
                log.info("✓ Bean encontrado: {}", bean.getClass().getName());
                resultado.put("cpf_mapper_classe", bean.getClass().getName());
            } catch (Exception e) {
                log.error("❌ Erro ao buscar bean: {}", e.getMessage());
                resultado.put("cpf_mapper_erro", e.getMessage());
            }
        }

        // Verificar pacotes escaneados
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        log.info("\nTotal de beans no contexto: {}", beanNames.length);
        resultado.put("total_beans_contexto", beanNames.length);

        // Buscar beans do pacote jsecurity.config
        List<String> configBeans = new ArrayList<>();
        for (String beanName : beanNames) {
            if (beanName.toLowerCase().contains("protocol") ||
                    beanName.toLowerCase().contains("cpf") ||
                    beanName.toLowerCase().contains("mapper")) {
                configBeans.add(beanName);
                log.info("  Bean relacionado: {}", beanName);
            }
        }
        resultado.put("beans_relacionados", configBeans);

        return ResponseEntity.ok(resultado);
    }

    /**
     * Força o registro manual de um Protocol Mapper
     */
    @GetMapping("/forcar-registro-mapper")
    public ResponseEntity<Map<String, Object>> forcarRegistroMapper() {
        log.info("=== FORÇANDO REGISTRO DE PROTOCOL MAPPER ===");

        Map<String, Object> resultado = new HashMap<>();

        try {
            // Buscar o bean
            if (!applicationContext.containsBean("cpfProtocolMapperConfiguration")) {
                resultado.put("status", "ERRO");
                resultado.put("mensagem", "Bean cpfProtocolMapperConfiguration não encontrado");
                return ResponseEntity.status(404).body(resultado);
            }

            AbstractProtocolMapperConfiguration mapper =
                    (AbstractProtocolMapperConfiguration) applicationContext.getBean("cpfProtocolMapperConfiguration");

            log.info("✓ Bean encontrado: {}", mapper.getClass().getName());

            // Buscar o registrador
            if (!applicationContext.containsBean("protocolMapperRegister")) {
                resultado.put("status", "ERRO");
                resultado.put("mensagem", "ProtocolMapperRegister não encontrado");
                return ResponseEntity.status(404).body(resultado);
            }

            Object register = applicationContext.getBean("protocolMapperRegister");
            log.info("✓ ProtocolMapperRegister encontrado: {}", register.getClass().getName());

            // Tentar executar o registro via reflection
            register.getClass().getMethod("register").invoke(register);

            resultado.put("status", "SUCESSO");
            resultado.put("mensagem", "Registro executado manualmente");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Erro ao forçar registro: {}", e.getMessage(), e);
            resultado.put("status", "ERRO");
            resultado.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(resultado);
        }
    }
}