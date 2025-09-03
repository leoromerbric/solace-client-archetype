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
 * Servicio para publicar mensajes en tópicos de Solace.
 * 
 * Este servicio proporciona funcionalidades para enviar mensajes a tópicos
 * de Solace con manejo de errores y reintentos.
 */
@Service
public class TopicPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(TopicPublisherService.class);

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
     * Publica un mensaje en un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @param message mensaje a publicar
     * @return MessageResponse con el resultado de la operación
     */
    @Retryable(
            retryFor = {JCSMPException.class, JsonProcessingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public MessageResponse publishToTopic(String topicName, SolaceMessage message) {
        try {
            // Configurar el destino del mensaje
            message.setDestination(topicName);
            
            // Crear el tópico
            Topic topic = solaceConfig.createTopic(topicName);
            
            // Crear el mensaje de texto
            TextMessage textMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            
            // Convertir el mensaje a JSON
            String jsonMessage = objectMapper.writeValueAsString(message);
            textMessage.setText(jsonMessage);
            
            // Configurar headers del mensaje
            if (message.getHeaders() != null) {
                message.getHeaders().forEach((key, value) -> {
                    try {
                        textMessage.setApplicationMessageId(message.getId());
                        textMessage.setCorrelationId(message.getCorrelationId());
                    } catch (Exception e) {
                        logger.warn("No se pudo configurar el header {}: {}", key, e.getMessage());
                    }
                });
            }
            
            // Configurar propiedades del mensaje
            textMessage.setApplicationMessageId(message.getId());
            textMessage.setApplicationMessageType(message.getType());
            textMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
            
            // Configurar correlation ID si existe
            if (message.getCorrelationId() != null) {
                textMessage.setCorrelationId(message.getCorrelationId());
            }
            
            // Enviar el mensaje
            messageProducer.send(textMessage, topic);
            
            logger.info("Mensaje publicado exitosamente en tópico: {} con ID: {}", topicName, message.getId());
            
            return MessageResponse.success(
                "Mensaje publicado exitosamente en tópico",
                message.getId(),
                topicName
            );
            
        } catch (JCSMPException e) {
            logger.error("Error al publicar mensaje en tópico {}: {}", topicName, e.getMessage(), e);
            return MessageResponse.error(
                "Error al publicar mensaje en tópico",
                "JCSMPException: " + e.getMessage()
            );
        } catch (JsonProcessingException e) {
            logger.error("Error al serializar mensaje para tópico {}: {}", topicName, e.getMessage(), e);
            return MessageResponse.error(
                "Error al serializar mensaje",
                "JsonProcessingException: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error inesperado al publicar mensaje en tópico {}: {}", topicName, e.getMessage(), e);
            return MessageResponse.error(
                "Error inesperado al publicar mensaje",
                e.getMessage()
            );
        }
    }

    /**
     * Publica un mensaje en el tópico de notificaciones configurado.
     * 
     * @param message mensaje a publicar
     * @return MessageResponse con el resultado de la operación
     */
    public MessageResponse publishNotification(SolaceMessage message) {
        String topicName = solaceProperties.getTopics().getNotification();
        logger.info("Publicando notificación en tópico: {}", topicName);
        return publishToTopic(topicName, message);
    }

    /**
     * Publica un mensaje en el tópico de eventos configurado.
     * 
     * @param message mensaje a publicar
     * @return MessageResponse con el resultado de la operación
     */
    public MessageResponse publishEvent(SolaceMessage message) {
        String topicName = solaceProperties.getTopics().getEvents();
        logger.info("Publicando evento en tópico: {}", topicName);
        return publishToTopic(topicName, message);
    }
}