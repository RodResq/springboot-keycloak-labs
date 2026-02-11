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

@Slf4j
@RestController
@RequestMapping("/api/v1/diagnostico")
public class BeanDiagnosticoController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private Collection<AbstractProtocolMapperConfiguration> protocolMappers;

    @GetMapping("/protocol-mappers")
    public ResponseEntity<Map<String, Object>> verificarProtocolMappers() {
        Map<String, Object> resultado = new HashMap<>();

        if (protocolMappers == null) {
            resultado.put("injecao_direta", "NULL");
        } else if (protocolMappers.isEmpty()) {
            resultado.put("injecao_direta", "VAZIO");
            resultado.put("tamanho", 0);
        } else {
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

        Map<String, AbstractProtocolMapperConfiguration> beansFound =
                applicationContext.getBeansOfType(AbstractProtocolMapperConfiguration.class);

        if (beansFound.isEmpty()) {
            resultado.put("beans_no_contexto", "NENHUM");
        } else {
            resultado.put("beans_no_contexto", beansFound.size());

            List<Map<String, String>> beansList = new ArrayList<>();
            beansFound.forEach((beanName, bean) -> {
                Map<String, String> beanInfo = new HashMap<>();
                beanInfo.put("nome", beanName);
                beanInfo.put("classe", bean.getClass().getName());
                beansList.add(beanInfo);
            });
            resultado.put("beans_detalhes", beansList);
        }

        boolean cpfMapperExists = applicationContext.containsBean("cpfProtocolMapperConfiguration");
        resultado.put("cpf_mapper_existe", cpfMapperExists);

        if (cpfMapperExists) {
            try {
                Object bean = applicationContext.getBean("cpfProtocolMapperConfiguration");
                resultado.put("cpf_mapper_classe", bean.getClass().getName());
            } catch (Exception e) {
                resultado.put("cpf_mapper_erro", e.getMessage());
            }
        }

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        resultado.put("total_beans_contexto", beanNames.length);

        List<String> configBeans = new ArrayList<>();
        for (String beanName : beanNames) {
            if (beanName.toLowerCase().contains("protocol") ||
                    beanName.toLowerCase().contains("cpf") ||
                    beanName.toLowerCase().contains("mapper")) {
                configBeans.add(beanName);
            }
        }
        resultado.put("beans_relacionados", configBeans);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/forcar-registro-mapper")
    public ResponseEntity<Map<String, Object>> forcarRegistroMapper() {
        Map<String, Object> resultado = new HashMap<>();

        try {
            if (!applicationContext.containsBean("cpfProtocolMapperConfiguration")) {
                resultado.put("status", "ERRO");
                resultado.put("mensagem", "Bean cpfProtocolMapperConfiguration não encontrado");
                return ResponseEntity.status(404).body(resultado);
            }

            AbstractProtocolMapperConfiguration mapper =
                    (AbstractProtocolMapperConfiguration) applicationContext.getBean("cpfProtocolMapperConfiguration");

            if (!applicationContext.containsBean("protocolMapperRegister")) {
                resultado.put("status", "ERRO");
                resultado.put("mensagem", "ProtocolMapperRegister não encontrado");
                return ResponseEntity.status(404).body(resultado);
            }

            Object register = applicationContext.getBean("protocolMapperRegister");
            register.getClass().getMethod("register").invoke(register);

            resultado.put("status", "SUCESSO");
            resultado.put("mensagem", "Registro executado manualmente");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            resultado.put("status", "ERRO");
            resultado.put("mensagem", e.getMessage());
            return ResponseEntity.status(500).body(resultado);
        }
    }
}