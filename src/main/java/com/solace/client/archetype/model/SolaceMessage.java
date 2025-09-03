package com.solace.client.archetype.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Modelo base para mensajes de Solace.
 * 
 * Esta clase proporciona la estructura común para todos los mensajes
 * que se envían y reciben a través de Solace.
 */
public class SolaceMessage {

    @JsonProperty("id")
    private String id;

    @NotBlank(message = "El tipo de mensaje es obligatorio")
    @JsonProperty("type")
    private String type;

    @NotNull(message = "El payload es obligatorio")
    @JsonProperty("payload")
    private Object payload;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * Constructor por defecto.
     */
    public SolaceMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.source = "solace-client-archetype";
    }

    /**
     * Constructor con parámetros básicos.
     * 
     * @param type tipo del mensaje
     * @param payload contenido del mensaje
     */
    public SolaceMessage(String type, Object payload) {
        this();
        this.type = type;
        this.payload = payload;
    }

    /**
     * Constructor completo.
     * 
     * @param type tipo del mensaje
     * @param payload contenido del mensaje
     * @param destination destino del mensaje
     */
    public SolaceMessage(String type, Object payload, String destination) {
        this(type, payload);
        this.destination = destination;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "SolaceMessage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}