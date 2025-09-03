package com.solace.client.archetype.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solace.client.archetype.config.SolaceConfig;
import com.solace.client.archetype.config.SolaceProperties;
import com.solace.client.archetype.model.SolaceMessage;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para consumir mensajes de tópicos de Solace.
 * 
 * Este servicio proporciona funcionalidades para suscribirse y consumir mensajes
 * de tópicos de Solace con manejo de errores.
 */
@Service
public class TopicConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(TopicConsumerService.class);

    @Autowired
    private JCSMPSession jcsmpSession;

    @Autowired
    private SolaceConfig solaceConfig;

    @Autowired
    private SolaceProperties solaceProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private XMLMessageConsumer topicConsumer;
    private final ConcurrentHashMap<String, List<SolaceMessage>> consumedMessages = new ConcurrentHashMap<>();

    /**
     * Inicializa el consumidor de tópicos al arrancar el servicio.
     */
    @PostConstruct
    public void initializeTopicConsumer() {
        try {
            // Crear el consumidor de mensajes
            topicConsumer = jcsmpSession.getMessageConsumer(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage message) {
                    processTopicMessage(message);
                }

                @Override
                public void onException(JCSMPException exception) {
                    logger.error("Excepción en consumidor de tópicos: {}", exception.getMessage(), exception);
                }
            });

            // Suscribirse a los tópicos configurados
            subscribeToConfiguredTopics();

            // Iniciar el consumidor
            topicConsumer.start();
            
            logger.info("Consumidor de tópicos inicializado y activo");

        } catch (JCSMPException e) {
            logger.error("Error al inicializar consumidor de tópicos: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar el consumidor de tópicos", e);
        }
    }

    /**
     * Se suscribe a los tópicos configurados en las propiedades.
     */
    private void subscribeToConfiguredTopics() throws JCSMPException {
        // Suscribirse al tópico de notificaciones
        String notificationTopic = solaceProperties.getTopics().getNotification();
        if (notificationTopic != null && !notificationTopic.isEmpty()) {
            subscribeToTopic(notificationTopic);
        }

        // Suscribirse al tópico de eventos
        String eventsTopic = solaceProperties.getTopics().getEvents();
        if (eventsTopic != null && !eventsTopic.isEmpty()) {
            subscribeToTopic(eventsTopic);
        }
    }

    /**
     * Se suscribe a un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @throws JCSMPException si hay problemas en la suscripción
     */
    public void subscribeToTopic(String topicName) throws JCSMPException {
        Topic topic = solaceConfig.createTopic(topicName);
        jcsmpSession.addSubscription(topic);
        
        // Inicializar la lista de mensajes para este tópico
        consumedMessages.putIfAbsent(topicName, new ArrayList<>());
        
        logger.info("Suscrito al tópico: {}", topicName);
    }

    /**
     * Se desuscribe de un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @throws JCSMPException si hay problemas en la desuscripción
     */
    public void unsubscribeFromTopic(String topicName) throws JCSMPException {
        Topic topic = solaceConfig.createTopic(topicName);
        jcsmpSession.removeSubscription(topic);
        
        // Limpiar la lista de mensajes para este tópico
        consumedMessages.remove(topicName);
        
        logger.info("Desuscrito del tópico: {}", topicName);
    }

    /**
     * Procesa un mensaje recibido de un tópico.
     * 
     * @param message mensaje recibido
     */
    private void processTopicMessage(BytesXMLMessage message) {
        try {
            // Extraer información del mensaje
            String messageText = null;
            String topicName = message.getDestination().getName();
            
            if (message instanceof TextMessage) {
                messageText = ((TextMessage) message).getText();
            } else {
                logger.warn("Mensaje recibido no es de tipo TextMessage en tópico: {}", topicName);
                return;
            }

            // Deserializar el mensaje
            SolaceMessage solaceMessage = objectMapper.readValue(messageText, SolaceMessage.class);
            
            // Agregar información adicional del mensaje recibido
            solaceMessage.setDestination(topicName);
            
            // Almacenar el mensaje consumido
            List<SolaceMessage> messages = consumedMessages.computeIfAbsent(topicName, k -> new ArrayList<>());
            messages.add(solaceMessage);
            
            // Mantener solo los últimos 100 mensajes por tópico
            if (messages.size() > 100) {
                messages.remove(0);
            }

            logger.info("Mensaje procesado desde tópico {}: ID={}, Tipo={}", 
                       topicName, solaceMessage.getId(), solaceMessage.getType());

            // Procesar el mensaje según su tipo
            processMessageByType(solaceMessage, topicName);

        } catch (Exception e) {
            logger.error("Error al procesar mensaje de tópico: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesa el mensaje según su tipo.
     * 
     * @param message mensaje a procesar
     * @param topicName nombre del tópico
     */
    private void processMessageByType(SolaceMessage message, String topicName) {
        switch (message.getType().toUpperCase()) {
            case "NOTIFICATION":
                logger.info("Procesando notificación desde tópico {}: {}", topicName, message.getPayload());
                break;
            case "EVENT":
                logger.info("Procesando evento desde tópico {}: {}", topicName, message.getPayload());
                break;
            default:
                logger.info("Procesando mensaje genérico desde tópico {}: {}", topicName, message.getPayload());
                break;
        }
    }

    /**
     * Obtiene los mensajes consumidos de un tópico específico.
     * 
     * @param topicName nombre del tópico
     * @return lista de mensajes consumidos
     */
    public List<SolaceMessage> getConsumedMessages(String topicName) {
        return new ArrayList<>(consumedMessages.getOrDefault(topicName, new ArrayList<>()));
    }

    /**
     * Obtiene todos los mensajes consumidos de todos los tópicos.
     * 
     * @return mapa con los mensajes consumidos por tópico
     */
    public ConcurrentHashMap<String, List<SolaceMessage>> getAllConsumedMessages() {
        return new ConcurrentHashMap<>(consumedMessages);
    }

    /**
     * Limpia los mensajes consumidos de un tópico específico.
     * 
     * @param topicName nombre del tópico
     */
    public void clearConsumedMessages(String topicName) {
        consumedMessages.remove(topicName);
        logger.info("Mensajes consumidos limpiados para tópico: {}", topicName);
    }

    /**
     * Limpia el consumidor al finalizar el servicio.
     */
    @PreDestroy
    public void cleanup() {
        try {
            if (topicConsumer != null) {
                topicConsumer.stop();
                topicConsumer.close();
                logger.info("Consumidor de tópicos detenido y cerrado");
            }
        } catch (Exception e) {
            logger.error("Error al limpiar consumidor de tópicos: {}", e.getMessage(), e);
        }
    }
}