package com.solace.client.archetype.controller;

import com.solace.client.archetype.model.MessageResponse;
import com.solace.client.archetype.model.SolaceMessage;
import com.solace.client.archetype.service.TopicPublisherService;
import com.solace.client.archetype.service.TopicConsumerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para operaciones con tópicos de Solace.
 * 
 * Este controlador proporciona endpoints para publicar mensajes en tópicos
 * y consultar mensajes consumidos de tópicos.
 */
@RestController
@RequestMapping("/topics")
@Tag(name = "Tópicos", description = "API para operaciones con tópicos de Solace")
public class TopicController {

    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Autowired
    private TopicPublisherService topicPublisherService;

    @Autowired
    private TopicConsumerService topicConsumerService;

    /**
     * Publica un mensaje en un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @param message mensaje a publicar
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/{topicName}/publish")
    @Operation(summary = "Publicar mensaje en tópico", 
               description = "Publica un mensaje en el tópico especificado")
    public ResponseEntity<MessageResponse> publishToTopic(
            @PathVariable String topicName,
            @Valid @RequestBody SolaceMessage message) {
        
        try {
            logger.info("Recibida petición para publicar mensaje en tópico: {}", topicName);
            
            MessageResponse response = topicPublisherService.publishToTopic(topicName, message);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al publicar mensaje en tópico {}: {}", topicName, e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al publicar mensaje",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Publica una notificación en el tópico de notificaciones configurado.
     * 
     * @param message mensaje de notificación
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/notifications/publish")
    @Operation(summary = "Publicar notificación", 
               description = "Publica una notificación en el tópico de notificaciones configurado")
    public ResponseEntity<MessageResponse> publishNotification(@Valid @RequestBody SolaceMessage message) {
        
        try {
            logger.info("Recibida petición para publicar notificación");
            
            MessageResponse response = topicPublisherService.publishNotification(message);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al publicar notificación: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al publicar notificación",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Publica un evento en el tópico de eventos configurado.
     * 
     * @param message mensaje de evento
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/events/publish")
    @Operation(summary = "Publicar evento", 
               description = "Publica un evento en el tópico de eventos configurado")
    public ResponseEntity<MessageResponse> publishEvent(@Valid @RequestBody SolaceMessage message) {
        
        try {
            logger.info("Recibida petición para publicar evento");
            
            MessageResponse response = topicPublisherService.publishEvent(message);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al publicar evento: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al publicar evento",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Suscribirse a un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/{topicName}/subscribe")
    @Operation(summary = "Suscribirse a tópico", 
               description = "Se suscribe al tópico especificado para consumir mensajes")
    public ResponseEntity<MessageResponse> subscribeToTopic(@PathVariable String topicName) {
        
        try {
            logger.info("Recibida petición para suscribirse a tópico: {}", topicName);
            
            topicConsumerService.subscribeToTopic(topicName);
            
            MessageResponse response = MessageResponse.success(
                "Suscripción exitosa al tópico",
                null,
                topicName
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al suscribirse a tópico {}: {}", topicName, e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error al suscribirse al tópico",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Desuscribirse de un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @return respuesta con el resultado de la operación
     */
    @DeleteMapping("/{topicName}/unsubscribe")
    @Operation(summary = "Desuscribirse de tópico", 
               description = "Se desuscribe del tópico especificado")
    public ResponseEntity<MessageResponse> unsubscribeFromTopic(@PathVariable String topicName) {
        
        try {
            logger.info("Recibida petición para desuscribirse de tópico: {}", topicName);
            
            topicConsumerService.unsubscribeFromTopic(topicName);
            
            MessageResponse response = MessageResponse.success(
                "Desuscripción exitosa del tópico",
                null,
                topicName
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al desuscribirse de tópico {}: {}", topicName, e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error al desuscribirse del tópico",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtiene los mensajes consumidos de un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @return lista de mensajes consumidos
     */
    @GetMapping("/{topicName}/consumed")
    @Operation(summary = "Obtener mensajes consumidos", 
               description = "Obtiene los mensajes consumidos del tópico especificado")
    public ResponseEntity<List<SolaceMessage>> getConsumedMessages(@PathVariable String topicName) {
        
        try {
            logger.info("Recibida petición para obtener mensajes consumidos de tópico: {}", topicName);
            
            List<SolaceMessage> messages = topicConsumerService.getConsumedMessages(topicName);
            
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            logger.error("Error al obtener mensajes consumidos de tópico {}: {}", topicName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene todos los mensajes consumidos de todos los tópicos.
     * 
     * @return mapa con mensajes consumidos por tópico
     */
    @GetMapping("/consumed/all")
    @Operation(summary = "Obtener todos los mensajes consumidos", 
               description = "Obtiene todos los mensajes consumidos de todos los tópicos")
    public ResponseEntity<Map<String, List<SolaceMessage>>> getAllConsumedMessages() {
        
        try {
            logger.info("Recibida petición para obtener todos los mensajes consumidos");
            
            Map<String, List<SolaceMessage>> allMessages = topicConsumerService.getAllConsumedMessages();
            
            return ResponseEntity.ok(allMessages);
            
        } catch (Exception e) {
            logger.error("Error al obtener todos los mensajes consumidos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Limpia los mensajes consumidos de un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @return respuesta con el resultado de la operación
     */
    @DeleteMapping("/{topicName}/consumed")
    @Operation(summary = "Limpiar mensajes consumidos", 
               description = "Limpia los mensajes consumidos del tópico especificado")
    public ResponseEntity<MessageResponse> clearConsumedMessages(@PathVariable String topicName) {
        
        try {
            logger.info("Recibida petición para limpiar mensajes consumidos de tópico: {}", topicName);
            
            topicConsumerService.clearConsumedMessages(topicName);
            
            MessageResponse response = MessageResponse.success(
                "Mensajes consumidos limpiados exitosamente",
                null,
                topicName
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al limpiar mensajes consumidos de tópico {}: {}", topicName, e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error al limpiar mensajes consumidos",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}