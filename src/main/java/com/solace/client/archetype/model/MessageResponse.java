package com.solace.client.archetype.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Modelo para respuestas de las operaciones de mensajería.
 * 
 * Esta clase encapsula la respuesta de las operaciones realizadas
 * con el broker Solace.
 */
public class MessageResponse {

    @JsonProperty("success")
    private boolean success;

    @NotBlank(message = "El mensaje es obligatorio")
    @JsonProperty("message")
    private String message;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("error")
    private String error;

    /**
     * Constructor por defecto.
     */
    public MessageResponse() {
    }

    /**
     * Constructor para respuesta exitosa.
     * 
     * @param success estado de la operación
     * @param message mensaje descriptivo
     * @param messageId ID del mensaje
     */
    public MessageResponse(boolean success, String message, String messageId) {
        this.success = success;
        this.message = message;
        this.messageId = messageId;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    /**
     * Crea una respuesta exitosa.
     * 
     * @param message mensaje descriptivo
     * @param messageId ID del mensaje
     * @return MessageResponse exitosa
     */
    public static MessageResponse success(String message, String messageId) {
        return new MessageResponse(true, message, messageId);
    }

    /**
     * Crea una respuesta exitosa con destino.
     * 
     * @param message mensaje descriptivo
     * @param messageId ID del mensaje
     * @param destination destino del mensaje
     * @return MessageResponse exitosa
     */
    public static MessageResponse success(String message, String messageId, String destination) {
        MessageResponse response = new MessageResponse(true, message, messageId);
        response.setDestination(destination);
        return response;
    }

    /**
     * Crea una respuesta de error.
     * 
     * @param message mensaje descriptivo
     * @param errorMessage descripción del error
     * @return MessageResponse con error
     */
    public static MessageResponse error(String message, String errorMessage) {
        MessageResponse response = new MessageResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setError(errorMessage);
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        return response;
    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", messageId='" + messageId + '\'' +
                ", destination='" + destination + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}