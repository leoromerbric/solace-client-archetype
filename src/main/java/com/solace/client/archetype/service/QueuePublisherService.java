package com.solace.client.archetype.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solace.client.archetype.config.SolaceConfig;
import com.solace.client.archetype.config.SolaceProperties;
import com.solace.client.archetype.model.MessageResponse;
import com.solace.client.archetype.model.SolaceMessage;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Servicio para publicar mensajes en colas de Solace.
 * 
 * Este servicio proporciona funcionalidades para enviar mensajes a colas
 * de Solace con manejo de errores y reintentos.
 */
@Service
public class QueuePublisherService {

    private static final Logger logger = LoggerFactory.getLogger(QueuePublisherService.class);

    @Autowired
    private JCSMPSession jcsmpSession;

    @Autowired
    private XMLMessageProducer messageProducer;

    @Autowired
    private SolaceConfig solaceConfig;

    @Autowired
    private SolaceProperties solaceProperties;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publica un mensaje en una cola específica.
     * 
     * @param queueName nombre de la cola
     * @param message mensaje a publicar
     * @return MessageResponse con el resultado de la operación
     */
    @Retryable(
            retryFor = {JCSMPException.class, JsonProcessingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public MessageResponse publishToQueue(String queueName, SolaceMessage message) {
        try {
            // Configurar el destino del mensaje
            message.setDestination(queueName);
            
            // Crear la cola
            Queue queue = solaceConfig.createQueue(queueName);
            
            // Crear el mensaje de texto
            TextMessage textMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            
            // Convertir el mensaje a JSON
            String jsonMessage = objectMapper.writeValueAsString(message);
            textMessage.setText(jsonMessage);
            
            // Configurar propiedades del mensaje
            textMessage.setApplicationMessageId(message.getId());
            textMessage.setApplicationMessageType(message.getType());
            textMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
            textMessage.setTimeToLive(300000); // 5 minutos TTL
            
            // Configurar correlation ID si existe
            if (message.getCorrelationId() != null) {
                textMessage.setCorrelationId(message.getCorrelationId());
            }
            
            // Configurar headers del mensaje
            if (message.getHeaders() != null) {
                message.getHeaders().forEach((key, value) -> {
                    try {
                        // Note: JCSMP doesn't support arbitrary properties on TextMessage
                        // We'll include them in the message payload instead
                        logger.debug("Header {} será incluido en el payload del mensaje", key);
                    } catch (Exception e) {
                        logger.warn("No se pudo configurar el header {}: {}", key, e.getMessage());
                    }
                });
            }
            
            // Enviar el mensaje
            messageProducer.send(textMessage, queue);
            
            logger.info("Mensaje publicado exitosamente en cola: {} con ID: {}", queueName, message.getId());
            
            return MessageResponse.success(
                "Mensaje publicado exitosamente en cola",
                message.getId(),
                queueName
            );
            
        } catch (JCSMPException e) {
            logger.error("Error al publicar mensaje en cola {}: {}", queueName, e.getMessage(), e);
            
            // Intentar enviar a cola de error si la operación falla
            try {
                sendToDeadLetterQueue(message, e.getMessage());
            } catch (Exception dlqException) {
                logger.error("Error adicional al enviar mensaje a cola de error: {}", dlqException.getMessage());
            }
            
            return MessageResponse.error(
                "Error al publicar mensaje en cola",
                "JCSMPException: " + e.getMessage()
            );
        } catch (JsonProcessingException e) {
            logger.error("Error al serializar mensaje para cola {}: {}", queueName, e.getMessage(), e);
            return MessageResponse.error(
                "Error al serializar mensaje",
                "JsonProcessingException: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error inesperado al publicar mensaje en cola {}: {}", queueName, e.getMessage(), e);
            return MessageResponse.error(
                "Error inesperado al publicar mensaje",
                e.getMessage()
            );
        }
    }

    /**
     * Publica un mensaje en la cola de órdenes configurada.
     * 
     * @param message mensaje a publicar
     * @return MessageResponse con el resultado de la operación
     */
    public MessageResponse publishOrder(SolaceMessage message) {
        String queueName = solaceProperties.getQueues().getOrders();
        logger.info("Publicando orden en cola: {}", queueName);
        return publishToQueue(queueName, message);
    }

    /**
     * Publica un mensaje en la cola de notificaciones configurada.
     * 
     * @param message mensaje a publicar
     * @return MessageResponse con el resultado de la operación
     */
    public MessageResponse publishNotificationToQueue(SolaceMessage message) {
        String queueName = solaceProperties.getQueues().getNotifications();
        logger.info("Publicando notificación en cola: {}", queueName);
        return publishToQueue(queueName, message);
    }

    /**
     * Envía un mensaje a la cola de error (Dead Letter Queue).
     * 
     * @param originalMessage mensaje original que falló
     * @param errorMessage mensaje de error
     */
    private void sendToDeadLetterQueue(SolaceMessage originalMessage, String errorMessage) {
        try {
            String deadLetterQueueName = solaceProperties.getQueues().getDeadLetter();
            
            // Crear mensaje de error con información adicional
            SolaceMessage errorMessageObj = new SolaceMessage();
            errorMessageObj.setType("ERROR");
            errorMessageObj.setPayload(originalMessage);
            errorMessageObj.getHeaders().put("error-reason", errorMessage);
            errorMessageObj.getHeaders().put("original-destination", originalMessage.getDestination());
            errorMessageObj.getHeaders().put("error-timestamp", java.time.LocalDateTime.now().toString());
            
            // Crear la cola de error
            Queue deadLetterQueue = solaceConfig.createQueue(deadLetterQueueName);
            
            // Crear el mensaje de texto
            TextMessage textMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            String jsonMessage = objectMapper.writeValueAsString(errorMessageObj);
            textMessage.setText(jsonMessage);
            
            // Configurar propiedades del mensaje
            textMessage.setApplicationMessageId(errorMessageObj.getId());
            textMessage.setApplicationMessageType("ERROR");
            textMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
            
            // Enviar el mensaje a la cola de error
            messageProducer.send(textMessage, deadLetterQueue);
            
            logger.info("Mensaje enviado a cola de error: {} con ID: {}", deadLetterQueueName, errorMessageObj.getId());
            
        } catch (Exception e) {
            logger.error("Error crítico: No se pudo enviar mensaje a cola de error: {}", e.getMessage(), e);
        }
    }
}