package com.solace.client.archetype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Aplicación principal del microservicio Solace Client Archetype.
 * 
 * Este microservicio proporciona funcionalidades para:
 * - Publicar mensajes en tópicos y colas de Solace
 * - Consumir mensajes de tópicos y colas de Solace
 * - Implementar patrones Request-Response
 * - Manejo de errores y reintentos
 * 
 * @author Solace Client Archetype
 * @version 1.0.0
 */
@SpringBootApplication
@EnableRetry
public class SolaceClientArchetypeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolaceClientArchetypeApplication.class, args);
    }
}