package com.solace.client.archetype.exception;

import com.solace.client.archetype.model.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la aplicación.
 * 
 * Esta clase maneja todas las excepciones no controladas y proporciona
 * respuestas consistentes para los clientes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de validación de parámetros.
     * 
     * @param ex excepción de validación
     * @return respuesta con errores de validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        logger.warn("Error de validación: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Error de validación en los parámetros");
        response.put("errors", errors);
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja excepciones de argumentos ilegales.
     * 
     * @param ex excepción de argumento ilegal
     * @return respuesta de error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        logger.warn("Argumento ilegal: {}", ex.getMessage());
        
        MessageResponse errorResponse = MessageResponse.error(
            "Parámetro inválido",
            ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Maneja excepciones de tiempo de espera.
     * 
     * @param ex excepción de timeout
     * @return respuesta de error
     */
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<MessageResponse> handleTimeoutException(
            java.util.concurrent.TimeoutException ex) {
        
        logger.warn("Timeout en operación: {}", ex.getMessage());
        
        MessageResponse errorResponse = MessageResponse.error(
            "Tiempo de espera agotado",
            "La operación no se completó en el tiempo esperado"
        );

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    /**
     * Maneja excepciones de runtime generales.
     * 
     * @param ex excepción de runtime
     * @return respuesta de error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(
            RuntimeException ex) {
        
        logger.error("Error de runtime: {}", ex.getMessage(), ex);
        
        MessageResponse errorResponse = MessageResponse.error(
            "Error interno del sistema",
            "Se produjo un error inesperado. Por favor, contacte al administrador."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Maneja todas las demás excepciones no contempladas.
     * 
     * @param ex excepción general
     * @return respuesta de error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGeneralException(
            Exception ex) {
        
        logger.error("Error inesperado: {}", ex.getMessage(), ex);
        
        MessageResponse errorResponse = MessageResponse.error(
            "Error inesperado del sistema",
            "Se produjo un error inesperado. Por favor, contacte al administrador."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}