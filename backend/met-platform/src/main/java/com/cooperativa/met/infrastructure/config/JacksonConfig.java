package com.cooperativa.met.infrastructure.config;

import com.cooperativa.met.infrastructure.security.XssSanitizerDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule xssSanitizerModule() {
        SimpleModule module = new SimpleModule();
        // Registramos nuestro deserializador para que atrape TODOS los Strings que llegan a la API
        module.addDeserializer(String.class, new XssSanitizerDeserializer());
        return module;
    }
}
