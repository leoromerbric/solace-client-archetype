package com.solace.client.archetype.controller;

import com.solace.client.archetype.model.MessageResponse;
import com.solace.client.archetype.model.SolaceMessage;
import com.solace.client.archetype.service.QueuePublisherService;
import com.solace.client.archetype.service.QueueConsumerService;
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
 * Controlador REST para operaciones con colas de Solace.
 * 
 * Este controlador proporciona endpoints para publicar mensajes en colas
 * y consultar mensajes consumidos de colas.
 */
@RestController
@RequestMapping("/queues")
@Tag(name = "Colas", description = "API para operaciones con colas de Solace")
public class QueueController {

    private static final Logger logger = LoggerFactory.getLogger(QueueController.class);

    @Autowired
    private QueuePublisherService queuePublisherService;

    @Autowired
    private QueueConsumerService queueConsumerService;

    /**
     * Publica un mensaje en una cola específica.
     * 
     * @param queueName nombre de la cola
     * @param message mensaje a publicar
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/{queueName}/publish")
    @Operation(summary = "Publicar mensaje en cola", 
               description = "Publica un mensaje en la cola especificada")
    public ResponseEntity<MessageResponse> publishToQueue(
            @PathVariable String queueName,
            @Valid @RequestBody SolaceMessage message) {
        
        try {
            logger.info("Recibida petición para publicar mensaje en cola: {}", queueName);
            
            MessageResponse response = queuePublisherService.publishToQueue(queueName, message);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al publicar mensaje en cola {}: {}", queueName, e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al publicar mensaje",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Publica una orden en la cola de órdenes configurada.
     * 
     * @param message mensaje de orden
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/orders/publish")
    @Operation(summary = "Publicar orden", 
               description = "Publica una orden en la cola de órdenes configurada")
    public ResponseEntity<MessageResponse> publishOrder(@Valid @RequestBody SolaceMessage message) {
        
        try {
            logger.info("Recibida petición para publicar orden");
            
            // Configurar el tipo del mensaje como ORDER si no está establecido
            if (message.getType() == null || message.getType().isEmpty()) {
                message.setType("ORDER");
            }
            
            MessageResponse response = queuePublisherService.publishOrder(message);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al publicar orden: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al publicar orden",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Publica una notificación en la cola de notificaciones configurada.
     * 
     * @param message mensaje de notificación
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/notifications/publish")
    @Operation(summary = "Publicar notificación en cola", 
               description = "Publica una notificación en la cola de notificaciones configurada")
    public ResponseEntity<MessageResponse> publishNotificationToQueue(@Valid @RequestBody SolaceMessage message) {
        
        try {
            logger.info("Recibida petición para publicar notificación en cola");
            
            // Configurar el tipo del mensaje como NOTIFICATION si no está establecido
            if (message.getType() == null || message.getType().isEmpty()) {
                message.setType("NOTIFICATION");
            }
            
            MessageResponse response = queuePublisherService.publishNotificationToQueue(message);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al publicar notificación en cola: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al publicar notificación en cola",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtiene los mensajes consumidos de una cola específica.
     * 
     * @param queueName nombre de la cola
     * @return lista de mensajes consumidos
     */
    @GetMapping("/{queueName}/consumed")
    @Operation(summary = "Obtener mensajes consumidos de cola", 
               description = "Obtiene los mensajes consumidos de la cola especificada")
    public ResponseEntity<List<SolaceMessage>> getConsumedMessages(@PathVariable String queueName) {
        
        try {
            logger.info("Recibida petición para obtener mensajes consumidos de cola: {}", queueName);
            
            List<SolaceMessage> messages = queueConsumerService.getConsumedMessages(queueName);
            
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            logger.error("Error al obtener mensajes consumidos de cola {}: {}", queueName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene todos los mensajes consumidos de todas las colas.
     * 
     * @return mapa con mensajes consumidos por cola
     */
    @GetMapping("/consumed/all")
    @Operation(summary = "Obtener todos los mensajes consumidos de colas", 
               description = "Obtiene todos los mensajes consumidos de todas las colas")
    public ResponseEntity<Map<String, List<SolaceMessage>>> getAllConsumedMessages() {
        
        try {
            logger.info("Recibida petición para obtener todos los mensajes consumidos de colas");
            
            Map<String, List<SolaceMessage>> allMessages = queueConsumerService.getAllConsumedMessages();
            
            return ResponseEntity.ok(allMessages);
            
        } catch (Exception e) {
            logger.error("Error al obtener todos los mensajes consumidos de colas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Limpia los mensajes consumidos de una cola específica.
     * 
     * @param queueName nombre de la cola
     * @return respuesta con el resultado de la operación
     */
    @DeleteMapping("/{queueName}/consumed")
    @Operation(summary = "Limpiar mensajes consumidos de cola", 
               description = "Limpia los mensajes consumidos de la cola especificada")
    public ResponseEntity<MessageResponse> clearConsumedMessages(@PathVariable String queueName) {
        
        try {
            logger.info("Recibida petición para limpiar mensajes consumidos de cola: {}", queueName);
            
            queueConsumerService.clearConsumedMessages(queueName);
            
            MessageResponse response = MessageResponse.success(
                "Mensajes consumidos limpiados exitosamente",
                null,
                queueName
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al limpiar mensajes consumidos de cola {}: {}", queueName, e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error al limpiar mensajes consumidos",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtiene estadísticas de una cola específica.
     * 
     * @param queueName nombre de la cola
     * @return estadísticas de la cola
     */
    @GetMapping("/{queueName}/stats")
    @Operation(summary = "Obtener estadísticas de cola", 
               description = "Obtiene estadísticas de la cola especificada")
    public ResponseEntity<Map<String, Object>> getQueueStats(@PathVariable String queueName) {
        
        try {
            logger.info("Recibida petición para obtener estadísticas de cola: {}", queueName);
            
            List<SolaceMessage> messages = queueConsumerService.getConsumedMessages(queueName);
            
            Map<String, Object> stats = Map.of(
                "queueName", queueName,
                "messagesConsumed", messages.size(),
                "lastMessageTime", messages.isEmpty() ? null : 
                    messages.get(messages.size() - 1).getTimestamp(),
                "messageTypes", messages.stream()
                    .map(SolaceMessage::getType)
                    .distinct()
                    .toList()
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de cola {}: {}", queueName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}