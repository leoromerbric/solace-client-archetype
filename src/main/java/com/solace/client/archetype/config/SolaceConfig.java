package com.solace.client.archetype.config;

import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuración de Solace JMS.
 * 
 * Esta clase configura la conexión con el broker Solace y proporciona los beans
 * necesarios para la comunicación con Solace.
 */
@Configuration
public class SolaceConfig {

    private static final Logger logger = LoggerFactory.getLogger(SolaceConfig.class);

    @Autowired
    private SolaceProperties solaceProperties;

    /**
     * Configura la sesión JCSMP para la comunicación con Solace.
     * 
     * @return JCSMPSession configurada
     * @throws JCSMPException si hay problemas en la configuración
     */
    @Bean
    public JCSMPSession jcsmpSession() throws JCSMPException {
        final JCSMPProperties properties = new JCSMPProperties();
        
        // Configuración básica del broker
        properties.setProperty(JCSMPProperties.HOST, solaceProperties.getBroker().getHost());
        properties.setProperty(JCSMPProperties.VPN_NAME, solaceProperties.getBroker().getVpn());
        properties.setProperty(JCSMPProperties.USERNAME, solaceProperties.getBroker().getUsername());
        properties.setProperty(JCSMPProperties.PASSWORD, solaceProperties.getBroker().getPassword());
        
        // Configuración de reconexión automática
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);
        
        // Configuración de SSL (si es necesario)
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, false);
        
        final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
        
        session.connect();
        
        logger.info("Conexión establecida con Solace broker: {}", solaceProperties.getBroker().getHost());
        
        return session;
    }

    /**
     * Configura el producer para envío de mensajes.
     * 
     * @param session sesión JCSMP
     * @return XMLMessageProducer configurado
     * @throws JCSMPException si hay problemas en la configuración
     */
    @Bean
    public XMLMessageProducer messageProducer(JCSMPSession session) throws JCSMPException {
        return session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            @Override
            public void responseReceived(String messageID) {
                logger.debug("Mensaje enviado correctamente. ID: {}", messageID);
            }

            @Override
            public void handleError(String messageID, JCSMPException exception, long timestamp) {
                logger.error("Error enviando mensaje ID: {}. Error: {}", messageID, exception.getMessage());
            }
        });
    }

    /**
     * Configura el template de reintentos.
     * 
     * @return RetryTemplate configurado
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Política de reintentos
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(solaceProperties.getRetry().getMaxAttempts());
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Política de backoff exponencial
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(solaceProperties.getRetry().getInitialDelayMs());
        backOffPolicy.setMaxInterval(solaceProperties.getRetry().getMaxDelayMs());
        backOffPolicy.setMultiplier(solaceProperties.getRetry().getMultiplier());
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    /**
     * Crea un endpoint de tópico.
     * 
     * @param topicName nombre del tópico
     * @return Topic configurado
     */
    public Topic createTopic(String topicName) {
        return JCSMPFactory.onlyInstance().createTopic(topicName);
    }

    /**
     * Crea un endpoint de cola.
     * 
     * @param queueName nombre de la cola
     * @return Queue configurada
     */
    public Queue createQueue(String queueName) {
        return JCSMPFactory.onlyInstance().createQueue(queueName);
    }
}