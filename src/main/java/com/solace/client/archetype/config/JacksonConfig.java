package com.solace.client.archetype.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración para Jackson ObjectMapper.
 * 
 * Esta clase configura el ObjectMapper para el manejo correcto de JSON
 * y fechas en la aplicación.
 */
@Configuration
public class JacksonConfig {

    /**
     * Configura el ObjectMapper principal para la aplicación.
     * 
     * @return ObjectMapper configurado
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Registrar módulo para Java Time API
        mapper.registerModule(new JavaTimeModule());
        
        // Configurar para no escribir fechas como timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configurar para no fallar con propiedades desconocidas
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Configurar para no incluir propiedades nulas
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        
        return mapper;
    }
}