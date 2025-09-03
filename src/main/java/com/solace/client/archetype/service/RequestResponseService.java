package com.solace.client.archetype.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solace.client.archetype.config.SolaceConfig;
import com.solace.client.archetype.config.SolaceProperties;
import com.solace.client.archetype.model.SolaceMessage;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Servicio para implementar el patrón Request-Response con Solace.
 * 
 * Este servicio proporciona funcionalidades para enviar requests y recibir responses
 * usando un patrón asíncrono con correlation IDs.
 */
@Service
public class RequestResponseService {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseService.class);

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

    private XMLMessageConsumer responseConsumer;
    private Topic replyToTopic;
    private final ConcurrentHashMap<String, CompletableFuture<SolaceMessage>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Inicializa el servicio de Request-Response.
     */
    @PostConstruct
    public void initializeRequestResponseService() {
        try {
            // Crear el tópico de respuesta único para esta instancia
            String replyToTopicName = solaceProperties.getRequestResponse().getRequestTopic() + "/reply/" + 
                                     java.util.UUID.randomUUID().toString();
            replyToTopic = solaceConfig.createTopic(replyToTopicName);

            // Crear el consumidor de respuestas
            responseConsumer = jcsmpSession.getMessageConsumer(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage message) {
                    processResponseMessage(message);
                }

                @Override
                public void onException(JCSMPException exception) {
                    logger.error("Excepción en consumidor de respuestas: {}", exception.getMessage(), exception);
                }
            });

            // Suscribirse al tópico de respuestas
            jcsmpSession.addSubscription(replyToTopic);

            // Iniciar el consumidor
            responseConsumer.start();

            logger.info("Servicio Request-Response inicializado. Tópico de respuesta: {}", replyToTopicName);

        } catch (JCSMPException e) {
            logger.error("Error al inicializar servicio Request-Response: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar el servicio Request-Response", e);
        }
    }

    /**
     * Envía un request y espera la respuesta.
     * 
     * @param requestMessage mensaje de request
     * @return CompletableFuture con la respuesta
     */
    @Retryable(
            retryFor = {JCSMPException.class, JsonProcessingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public CompletableFuture<SolaceMessage> sendRequest(SolaceMessage requestMessage) {
        try {
            // Generar correlation ID único
            String correlationId = java.util.UUID.randomUUID().toString();
            requestMessage.setCorrelationId(correlationId);

            // Crear el Future para la respuesta
            CompletableFuture<SolaceMessage> responseFuture = new CompletableFuture<>();
            pendingRequests.put(correlationId, responseFuture);

            // Configurar timeout
            int timeoutMs = solaceProperties.getRequestResponse().getTimeoutMs();
            responseFuture.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                         .whenComplete((result, throwable) -> {
                             if (throwable instanceof TimeoutException) {
                                 logger.warn("Timeout en request con correlationId: {}", correlationId);
                                 pendingRequests.remove(correlationId);
                             }
                         });

            // Crear el tópico de request
            String requestTopicName = solaceProperties.getRequestResponse().getRequestTopic();
            Topic requestTopic = solaceConfig.createTopic(requestTopicName);

            // Crear el mensaje de texto
            TextMessage textMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            
            // Convertir el mensaje a JSON
            String jsonMessage = objectMapper.writeValueAsString(requestMessage);
            textMessage.setText(jsonMessage);

            // Configurar propiedades del mensaje
            textMessage.setApplicationMessageId(requestMessage.getId());
            textMessage.setApplicationMessageType(requestMessage.getType());
            textMessage.setCorrelationId(correlationId);
            textMessage.setDeliveryMode(DeliveryMode.DIRECT);
            textMessage.setReplyTo(replyToTopic);

            // Enviar el request
            messageProducer.send(textMessage, requestTopic);

            logger.info("Request enviado. MessageId: {}, CorrelationId: {}", 
                       requestMessage.getId(), correlationId);

            return responseFuture;

        } catch (JCSMPException e) {
            logger.error("Error al enviar request: {}", e.getMessage(), e);
            CompletableFuture<SolaceMessage> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        } catch (JsonProcessingException e) {
            logger.error("Error al serializar request: {}", e.getMessage(), e);
            CompletableFuture<SolaceMessage> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        }
    }

    /**
     * Envía un request y espera la respuesta de forma síncrona.
     * 
     * @param requestMessage mensaje de request
     * @return respuesta del request
     * @throws Exception si hay errores en el envío o timeout
     */
    public SolaceMessage sendRequestSync(SolaceMessage requestMessage) throws Exception {
        CompletableFuture<SolaceMessage> future = sendRequest(requestMessage);
        int timeoutMs = solaceProperties.getRequestResponse().getTimeoutMs();
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Procesa un mensaje de respuesta recibido.
     * 
     * @param message mensaje de respuesta
     */
    private void processResponseMessage(BytesXMLMessage message) {
        try {
            // Extraer correlation ID
            String correlationId = message.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                logger.warn("Mensaje de respuesta recibido sin correlation ID");
                return;
            }

            // Buscar el request pendiente
            CompletableFuture<SolaceMessage> pendingRequest = pendingRequests.remove(correlationId);
            if (pendingRequest == null) {
                logger.warn("No se encontró request pendiente para correlationId: {}", correlationId);
                return;
            }

            // Extraer el texto del mensaje
            String messageText = null;
            if (message instanceof TextMessage) {
                messageText = ((TextMessage) message).getText();
            } else {
                logger.warn("Mensaje de respuesta no es de tipo TextMessage");
                pendingRequest.completeExceptionally(new RuntimeException("Tipo de mensaje inválido"));
                return;
            }

            // Deserializar la respuesta
            SolaceMessage responseMessage = objectMapper.readValue(messageText, SolaceMessage.class);
            responseMessage.setCorrelationId(correlationId);

            // Completar el Future con la respuesta
            pendingRequest.complete(responseMessage);

            logger.info("Respuesta procesada para correlationId: {}", correlationId);

        } catch (Exception e) {
            logger.error("Error al procesar mensaje de respuesta: {}", e.getMessage(), e);
            
            // Intentar completar con excepción si hay correlation ID
            String correlationId = message.getCorrelationId();
            if (correlationId != null) {
                CompletableFuture<SolaceMessage> pendingRequest = pendingRequests.remove(correlationId);
                if (pendingRequest != null) {
                    pendingRequest.completeExceptionally(e);
                }
            }
        }
    }

    /**
     * Simula el procesamiento de requests como servidor (para testing).
     * Este método sería implementado por servicios que actúan como servidores.
     * 
     * @param requestTopic tópico donde escuchar requests
     * @param processor función para procesar requests
     */
    public void startRequestProcessor(String requestTopic, RequestProcessor processor) {
        try {
            // Crear consumidor para requests
            XMLMessageConsumer requestConsumer = jcsmpSession.getMessageConsumer(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage message) {
                    processIncomingRequest(message, processor);
                }

                @Override
                public void onException(JCSMPException exception) {
                    logger.error("Excepción en procesador de requests: {}", exception.getMessage(), exception);
                }
            });

            // Suscribirse al tópico de requests
            Topic topic = solaceConfig.createTopic(requestTopic);
            jcsmpSession.addSubscription(topic);

            // Iniciar el consumidor
            requestConsumer.start();

            logger.info("Procesador de requests iniciado para tópico: {}", requestTopic);

        } catch (JCSMPException e) {
            logger.error("Error al iniciar procesador de requests: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo iniciar el procesador de requests", e);
        }
    }

    /**
     * Procesa un request entrante y envía la respuesta.
     * 
     * @param message mensaje de request
     * @param processor procesador de requests
     */
    private void processIncomingRequest(BytesXMLMessage message, RequestProcessor processor) {
        try {
            // Extraer información del request
            String correlationId = message.getCorrelationId();
            Destination replyTo = message.getReplyTo();
            
            if (correlationId == null || replyTo == null) {
                logger.warn("Request recibido sin correlation ID o reply-to");
                return;
            }

            // Extraer el texto del mensaje
            String messageText = null;
            if (message instanceof TextMessage) {
                messageText = ((TextMessage) message).getText();
            } else {
                logger.warn("Request recibido no es de tipo TextMessage");
                return;
            }

            // Deserializar el request
            SolaceMessage requestMessage = objectMapper.readValue(messageText, SolaceMessage.class);

            // Procesar el request
            SolaceMessage responseMessage = processor.processRequest(requestMessage);
            responseMessage.setCorrelationId(correlationId);

            // Crear y enviar la respuesta
            TextMessage responseTextMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            String jsonResponse = objectMapper.writeValueAsString(responseMessage);
            responseTextMessage.setText(jsonResponse);
            responseTextMessage.setCorrelationId(correlationId);
            responseTextMessage.setApplicationMessageId(responseMessage.getId());
            responseTextMessage.setDeliveryMode(DeliveryMode.DIRECT);

            // Enviar la respuesta
            messageProducer.send(responseTextMessage, replyTo);

            logger.info("Respuesta enviada para correlationId: {}", correlationId);

        } catch (Exception e) {
            logger.error("Error al procesar request entrante: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtiene el número de requests pendientes.
     * 
     * @return número de requests pendientes
     */
    public int getPendingRequestsCount() {
        return pendingRequests.size();
    }

    /**
     * Limpia el servicio al finalizar.
     */
    @PreDestroy
    public void cleanup() {
        try {
            // Cancelar todos los requests pendientes
            pendingRequests.forEach((correlationId, future) -> {
                future.cancel(true);
            });
            pendingRequests.clear();

            if (responseConsumer != null) {
                responseConsumer.stop();
                responseConsumer.close();
                logger.info("Servicio Request-Response detenido y cerrado");
            }
        } catch (Exception e) {
            logger.error("Error al limpiar servicio Request-Response: {}", e.getMessage(), e);
        }
    }

    /**
     * Interfaz funcional para procesar requests.
     */
    @FunctionalInterface
    public interface RequestProcessor {
        SolaceMessage processRequest(SolaceMessage request);
    }
}