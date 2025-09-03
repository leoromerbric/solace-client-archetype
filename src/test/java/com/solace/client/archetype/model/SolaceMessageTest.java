package com.solace.client.archetype.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la clase SolaceMessage.
 */
class SolaceMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDefaultConstructor() {
        SolaceMessage message = new SolaceMessage();
        
        assertNotNull(message.getId());
        assertNotNull(message.getTimestamp());
        assertEquals("solace-client-archetype", message.getSource());
    }

    @Test
    void testConstructorWithTypeAndPayload() {
        String type = "TEST";
        String payload = "Test payload";
        
        SolaceMessage message = new SolaceMessage(type, payload);
        
        assertNotNull(message.getId());
        assertNotNull(message.getTimestamp());
        assertEquals(type, message.getType());
        assertEquals(payload, message.getPayload());
        assertEquals("solace-client-archetype", message.getSource());
    }

    @Test
    void testConstructorWithDestination() {
        String type = "TEST";
        String payload = "Test payload";
        String destination = "test.topic";
        
        SolaceMessage message = new SolaceMessage(type, payload, destination);
        
        assertEquals(type, message.getType());
        assertEquals(payload, message.getPayload());
        assertEquals(destination, message.getDestination());
    }

    @Test
    void testSettersAndGetters() {
        SolaceMessage message = new SolaceMessage();
        
        String id = "test-id";
        String type = "TEST_TYPE";
        Object payload = Map.of("key", "value");
        LocalDateTime timestamp = LocalDateTime.now();
        String source = "test-source";
        String destination = "test-destination";
        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "value1");
        String correlationId = "correlation-123";
        
        message.setId(id);
        message.setType(type);
        message.setPayload(payload);
        message.setTimestamp(timestamp);
        message.setSource(source);
        message.setDestination(destination);
        message.setHeaders(headers);
        message.setCorrelationId(correlationId);
        
        assertEquals(id, message.getId());
        assertEquals(type, message.getType());
        assertEquals(payload, message.getPayload());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(source, message.getSource());
        assertEquals(destination, message.getDestination());
        assertEquals(headers, message.getHeaders());
        assertEquals(correlationId, message.getCorrelationId());
    }

    @Test
    void testToString() {
        SolaceMessage message = new SolaceMessage("TEST", "payload");
        message.setDestination("test.destination");
        message.setCorrelationId("corr-123");
        
        String toString = message.toString();
        
        assertTrue(toString.contains("TEST"));
        assertTrue(toString.contains("test.destination"));
        assertTrue(toString.contains("corr-123"));
    }
}