package com.solace.client.archetype.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para la integración con Solace.
 * 
 * Esta clase mapea todas las propiedades de configuración relacionadas con Solace
 * desde el archivo application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "solace.client")
public class SolaceProperties {

    private Broker broker = new Broker();
    private Topics topics = new Topics();
    private Queues queues = new Queues();
    private RequestResponse requestResponse = new RequestResponse();
    private Retry retry = new Retry();

    // Getters y Setters
    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    public Queues getQueues() {
        return queues;
    }

    public void setQueues(Queues queues) {
        this.queues = queues;
    }

    public RequestResponse getRequestResponse() {
        return requestResponse;
    }

    public void setRequestResponse(RequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    /**
     * Configuración del broker Solace
     */
    public static class Broker {
        private String host;
        private String vpn;
        private String username;
        private String password;

        // Getters y Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getVpn() {
            return vpn;
        }

        public void setVpn(String vpn) {
            this.vpn = vpn;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Configuración de tópicos
     */
    public static class Topics {
        private String notification;
        private String events;

        // Getters y Setters
        public String getNotification() {
            return notification;
        }

        public void setNotification(String notification) {
            this.notification = notification;
        }

        public String getEvents() {
            return events;
        }

        public void setEvents(String events) {
            this.events = events;
        }
    }

    /**
     * Configuración de colas
     */
    public static class Queues {
        private String orders;
        private String notifications;
        private String deadLetter;

        // Getters y Setters
        public String getOrders() {
            return orders;
        }

        public void setOrders(String orders) {
            this.orders = orders;
        }

        public String getNotifications() {
            return notifications;
        }

        public void setNotifications(String notifications) {
            this.notifications = notifications;
        }

        public String getDeadLetter() {
            return deadLetter;
        }

        public void setDeadLetter(String deadLetter) {
            this.deadLetter = deadLetter;
        }
    }

    /**
     * Configuración de Request-Response
     */
    public static class RequestResponse {
        private String requestTopic;
        private int timeoutMs;

        // Getters y Setters
        public String getRequestTopic() {
            return requestTopic;
        }

        public void setRequestTopic(String requestTopic) {
            this.requestTopic = requestTopic;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * Configuración de reintentos
     */
    public static class Retry {
        private int maxAttempts;
        private long initialDelayMs;
        private long maxDelayMs;
        private double multiplier;

        // Getters y Setters
        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public long getMaxDelayMs() {
            return maxDelayMs;
        }

        public void setMaxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}