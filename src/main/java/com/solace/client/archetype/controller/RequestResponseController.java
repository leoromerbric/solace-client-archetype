package com.solace.client.archetype.controller;

import com.solace.client.archetype.model.MessageResponse;
import com.solace.client.archetype.model.SolaceMessage;
import com.solace.client.archetype.service.RequestResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controlador REST para operaciones Request-Response con Solace.
 * 
 * Este controlador proporciona endpoints para enviar requests y recibir responses
 * usando el patrón Request-Response.
 */
@RestController
@RequestMapping("/request-response")
@Tag(name = "Request-Response", description = "API para operaciones Request-Response con Solace")
public class RequestResponseController {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseController.class);

    @Autowired
    private RequestResponseService requestResponseService;

    /**
     * Envía un request y espera la respuesta de forma asíncrona.
     * 
     * @param requestMessage mensaje de request
     * @return respuesta asíncrona
     */
    @PostMapping("/send-async")
    @Operation(summary = "Enviar request asíncrono", 
               description = "Envía un request y retorna inmediatamente un CompletableFuture")
    public ResponseEntity<MessageResponse> sendRequestAsync(@Valid @RequestBody SolaceMessage requestMessage) {
        
        try {
            logger.info("Recibida petición para enviar request asíncrono");
            
            CompletableFuture<SolaceMessage> future = requestResponseService.sendRequest(requestMessage);
            
            // Configurar callbacks para la respuesta asíncrona
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    logger.error("Error en request asíncrono: {}", throwable.getMessage());
                } else {
                    logger.info("Response recibido para request asíncrono: {}", response.getId());
                }
            });
            
            MessageResponse response = MessageResponse.success(
                "Request enviado exitosamente (asíncrono)",
                requestMessage.getId()
            );
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error inesperado al enviar request asíncrono: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error inesperado al enviar request asíncrono",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Envía un request y espera la respuesta de forma síncrona.
     * 
     * @param requestMessage mensaje de request
     * @return respuesta del request
     */
    @PostMapping("/send-sync")
    @Operation(summary = "Enviar request síncrono", 
               description = "Envía un request y espera la respuesta de forma síncrona")
    public ResponseEntity<SolaceMessage> sendRequestSync(@Valid @RequestBody SolaceMessage requestMessage) {
        
        try {
            logger.info("Recibida petición para enviar request síncrono");
            
            SolaceMessage response = requestResponseService.sendRequestSync(requestMessage);
            
            logger.info("Response recibido para request síncrono: {}", response.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al enviar request síncrono: {}", e.getMessage(), e);
            
            // Crear una respuesta de error
            SolaceMessage errorResponse = new SolaceMessage();
            errorResponse.setType("ERROR");
            errorResponse.setPayload("Error al procesar request: " + e.getMessage());
            errorResponse.setCorrelationId(requestMessage.getCorrelationId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Simula un servidor que procesa requests y envía responses.
     * Este endpoint inicia un procesador de requests para testing.
     * 
     * @param topicName tópico donde escuchar requests
     * @return respuesta con el resultado de la operación
     */
    @PostMapping("/start-server/{topicName}")
    @Operation(summary = "Iniciar servidor de requests", 
               description = "Inicia un servidor que procesa requests en el tópico especificado")
    public ResponseEntity<MessageResponse> startRequestServer(@PathVariable String topicName) {
        
        try {
            logger.info("Recibida petición para iniciar servidor de requests en tópico: {}", topicName);
            
            // Procesador simple que responde con echo del request
            RequestResponseService.RequestProcessor processor = (request) -> {
                SolaceMessage response = new SolaceMessage();
                response.setType("RESPONSE");
                response.setPayload("Echo: " + request.getPayload());
                response.setCorrelationId(request.getCorrelationId());
                return response;
            };
            
            requestResponseService.startRequestProcessor(topicName, processor);
            
            MessageResponse response = MessageResponse.success(
                "Servidor de requests iniciado exitosamente",
                null,
                topicName
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al iniciar servidor de requests: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error(
                "Error al iniciar servidor de requests",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtiene estadísticas del servicio Request-Response.
     * 
     * @return estadísticas del servicio
     */
    @GetMapping("/stats")
    @Operation(summary = "Obtener estadísticas Request-Response", 
               description = "Obtiene estadísticas del servicio Request-Response")
    public ResponseEntity<Object> getRequestResponseStats() {
        
        try {
            logger.info("Recibida petición para obtener estadísticas Request-Response");
            
            int pendingRequests = requestResponseService.getPendingRequestsCount();
            
            Object stats = new Object() {
                public final int pendingRequests;
                public final String status;
                public final String timestamp;
                
                {
                    this.pendingRequests = requestResponseService.getPendingRequestsCount();
                    this.status = "active";
                    this.timestamp = java.time.LocalDateTime.now().toString();
                }
            };
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas Request-Response: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint de prueba para simular un request complejo.
     * 
     * @param payload contenido del request
     * @return respuesta del request
     */
    @PostMapping("/test")
    @Operation(summary = "Test Request-Response", 
               description = "Endpoint de prueba para el patrón Request-Response")
    public ResponseEntity<Object> testRequestResponse(@RequestBody Object payload) {
        
        try {
            logger.info("Recibida petición de test Request-Response");
            
            // Crear mensaje de test
            SolaceMessage testMessage = new SolaceMessage();
            testMessage.setType("TEST");
            testMessage.setPayload(payload);
            
            // Intentar enviar request síncrono con timeout corto
            try {
                SolaceMessage serviceResponse = requestResponseService.sendRequestSync(testMessage);
                
                Object result = new Object() {
                    public final boolean success;
                    public final String message;
                    public final SolaceMessage request;
                    public final SolaceMessage response;
                    
                    {
                        this.success = true;
                        this.message = "Test completado exitosamente";
                        this.request = testMessage;
                        this.response = serviceResponse;
                    }
                };
                
                return ResponseEntity.ok(result);
                
            } catch (Exception e) {
                // Si falla, retornar información del intento
                Object result = new Object() {
                    public final boolean success;
                    public final String message;
                    public final SolaceMessage request;
                    public final String error;
                    
                    {
                        this.success = false;
                        this.message = "Test completado con timeout o error";
                        this.request = testMessage;
                        this.error = e.getMessage();
                    }
                };
                
                return ResponseEntity.ok(result);
            }
            
        } catch (Exception e) {
            logger.error("Error en test Request-Response: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}