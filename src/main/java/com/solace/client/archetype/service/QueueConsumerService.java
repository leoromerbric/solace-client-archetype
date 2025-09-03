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
 * Servicio para consumir mensajes de colas de Solace.
 * 
 * Este servicio proporciona funcionalidades para consumir mensajes
 * de colas de Solace con manejo de errores y acknowledgment.
 */
@Service
public class QueueConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(QueueConsumerService.class);

    @Autowired
    private JCSMPSession jcsmpSession;

    @Autowired
    private SolaceConfig solaceConfig;

    @Autowired
    private SolaceProperties solaceProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, FlowReceiver> queueConsumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SolaceMessage>> consumedMessages = new ConcurrentHashMap<>();

    /**
     * Inicializa los consumidores de colas al arrancar el servicio.
     */
    @PostConstruct
    public void initializeQueueConsumers() {
        try {
            // Inicializar consumidores para las colas configuradas
            initializeConsumerForQueue(solaceProperties.getQueues().getOrders());
            initializeConsumerForQueue(solaceProperties.getQueues().getNotifications());
            
            logger.info("Consumidores de colas inicializados y activos");

        } catch (Exception e) {
            logger.error("Error al inicializar consumidores de colas: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudieron inicializar los consumidores de colas", e);
        }
    }

    /**
     * Inicializa un consumidor para una cola específica.
     * 
     * @param queueName nombre de la cola
     * @throws JCSMPException si hay problemas en la inicialización
     */
    private void initializeConsumerForQueue(String queueName) throws JCSMPException {
        if (queueName == null || queueName.isEmpty()) {
            logger.warn("Nombre de cola vacío, saltando inicialización");
            return;
        }

        // Crear la cola
        Queue queue = solaceConfig.createQueue(queueName);

        // Configurar propiedades del consumidor
        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);
        flowProps.setActiveFlowIndication(true);

        // Crear el listener para procesar mensajes
        XMLMessageListener messageListener = new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage message) {
                processQueueMessage(message, queueName);
            }

            @Override
            public void onException(JCSMPException exception) {
                logger.error("Excepción en consumidor de cola {}: {}", queueName, exception.getMessage(), exception);
            }
        };

        // Crear y iniciar el consumidor
        FlowReceiver flowReceiver = jcsmpSession.createFlow(messageListener, flowProps);
        flowReceiver.start();

        // Almacenar el consumidor
        queueConsumers.put(queueName, flowReceiver);
        
        // Inicializar la lista de mensajes para esta cola
        consumedMessages.putIfAbsent(queueName, new ArrayList<>());

        logger.info("Consumidor inicializado para cola: {}", queueName);
    }

    /**
     * Procesa un mensaje recibido de una cola.
     * 
     * @param message mensaje recibido
     * @param queueName nombre de la cola
     */
    private void processQueueMessage(BytesXMLMessage message, String queueName) {
        try {
            // Extraer información del mensaje
            String messageText = null;
            
            if (message instanceof TextMessage) {
                messageText = ((TextMessage) message).getText();
            } else {
                logger.warn("Mensaje recibido no es de tipo TextMessage en cola: {}", queueName);
                message.ackMessage(); // Acknowledger el mensaje inválido
                return;
            }

            // Deserializar el mensaje
            SolaceMessage solaceMessage = objectMapper.readValue(messageText, SolaceMessage.class);
            
            // Agregar información adicional del mensaje recibido
            solaceMessage.setDestination(queueName);
            
            // Almacenar el mensaje consumido
            List<SolaceMessage> messages = consumedMessages.computeIfAbsent(queueName, k -> new ArrayList<>());
            messages.add(solaceMessage);
            
            // Mantener solo los últimos 50 mensajes por cola
            if (messages.size() > 50) {
                messages.remove(0);
            }

            logger.info("Mensaje procesado desde cola {}: ID={}, Tipo={}", 
                       queueName, solaceMessage.getId(), solaceMessage.getType());

            // Procesar el mensaje según su tipo
            boolean processedSuccessfully = processMessageByType(solaceMessage, queueName);

            // Acknowledger el mensaje si fue procesado exitosamente
            if (processedSuccessfully) {
                message.ackMessage();
                logger.debug("Mensaje acknowledged en cola: {}", queueName);
            } else {
                // En caso de error, el mensaje no se acknowledges y será reentregado
                logger.warn("Mensaje no acknowledged en cola: {}. Será reentregado.", queueName);
            }

        } catch (Exception e) {
            logger.error("Error al procesar mensaje de cola {}: {}", queueName, e.getMessage(), e);
            
            // En caso de error, intentar acknowledger el mensaje para evitar loops infinitos
            try {
                message.ackMessage();
            } catch (Exception ackException) {
                logger.error("Error adicional al acknowledger mensaje con error: {}", ackException.getMessage());
            }
        }
    }

    /**
     * Procesa el mensaje según su tipo.
     * 
     * @param message mensaje a procesar
     * @param queueName nombre de la cola
     * @return true si el mensaje fue procesado exitosamente
     */
    private boolean processMessageByType(SolaceMessage message, String queueName) {
        try {
            switch (message.getType().toUpperCase()) {
                case "ORDER":
                    logger.info("Procesando orden desde cola {}: {}", queueName, message.getPayload());
                    // Aquí iría la lógica específica para procesar órdenes
                    return processOrder(message);
                    
                case "NOTIFICATION":
                    logger.info("Procesando notificación desde cola {}: {}", queueName, message.getPayload());
                    // Aquí iría la lógica específica para procesar notificaciones
                    return processNotification(message);
                    
                case "ERROR":
                    logger.info("Procesando mensaje de error desde cola {}: {}", queueName, message.getPayload());
                    // Aquí iría la lógica específica para procesar mensajes de error
                    return processErrorMessage(message);
                    
                default:
                    logger.info("Procesando mensaje genérico desde cola {}: {}", queueName, message.getPayload());
                    return true; // Procesar mensaje genérico siempre es exitoso
            }
        } catch (Exception e) {
            logger.error("Error al procesar mensaje de tipo {} desde cola {}: {}", 
                        message.getType(), queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Procesa una orden específica.
     * 
     * @param message mensaje de orden
     * @return true si fue procesado exitosamente
     */
    private boolean processOrder(SolaceMessage message) {
        // Simular procesamiento de orden
        try {
            // Aquí iría la lógica específica del negocio para procesar órdenes
            Thread.sleep(100); // Simular procesamiento
            logger.info("Orden procesada exitosamente: {}", message.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error procesando orden {}: {}", message.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Procesa una notificación específica.
     * 
     * @param message mensaje de notificación
     * @return true si fue procesado exitosamente
     */
    private boolean processNotification(SolaceMessage message) {
        // Simular procesamiento de notificación
        try {
            // Aquí iría la lógica específica del negocio para procesar notificaciones
            logger.info("Notificación procesada exitosamente: {}", message.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error procesando notificación {}: {}", message.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Procesa un mensaje de error.
     * 
     * @param message mensaje de error
     * @return true si fue procesado exitosamente
     */
    private boolean processErrorMessage(SolaceMessage message) {
        // Los mensajes de error siempre se procesan exitosamente para evitar loops
        logger.info("Mensaje de error registrado: {}", message.getId());
        return true;
    }

    /**
     * Obtiene los mensajes consumidos de una cola específica.
     * 
     * @param queueName nombre de la cola
     * @return lista de mensajes consumidos
     */
    public List<SolaceMessage> getConsumedMessages(String queueName) {
        return new ArrayList<>(consumedMessages.getOrDefault(queueName, new ArrayList<>()));
    }

    /**
     * Obtiene todos los mensajes consumidos de todas las colas.
     * 
     * @return mapa con los mensajes consumidos por cola
     */
    public ConcurrentHashMap<String, List<SolaceMessage>> getAllConsumedMessages() {
        return new ConcurrentHashMap<>(consumedMessages);
    }

    /**
     * Limpia los mensajes consumidos de una cola específica.
     * 
     * @param queueName nombre de la cola
     */
    public void clearConsumedMessages(String queueName) {
        consumedMessages.remove(queueName);
        logger.info("Mensajes consumidos limpiados para cola: {}", queueName);
    }

    /**
     * Limpia los consumidores al finalizar el servicio.
     */
    @PreDestroy
    public void cleanup() {
        queueConsumers.forEach((queueName, flowReceiver) -> {
            try {
                flowReceiver.stop();
                flowReceiver.close();
                logger.info("Consumidor de cola {} detenido y cerrado", queueName);
            } catch (Exception e) {
                logger.error("Error al limpiar consumidor de cola {}: {}", queueName, e.getMessage(), e);
            }
        });
        queueConsumers.clear();
    }
}