# Diagramas de Secuencia - Solace Client Archetype

## Introducción

Este documento contiene los diagramas de secuencia detallados que muestran el funcionamiento interno del sistema Solace Client Archetype. Los diagramas ilustran los flujos de mensajería para todos los patrones implementados.

---

## 1. Flujo de Publicación en Tópicos

### Escenario: Cliente publica mensaje en un tópico

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant TC as Topic Controller
    participant TPS as Topic Publisher Service
    participant SC as Solace Config
    participant SB as Solace Broker
    participant TCS as Topic Consumer Service
    participant Log as Application Logs

    Note over Client,Log: Flujo de Publicación en Tópicos

    Client->>+TC: POST /topics/{topicName}/publish
    Note right of Client: Content-Type: application/json<br/>Body: SolaceMessage

    TC->>TC: Validar request
    TC->>+TPS: publishToTopic(topicName, message)

    TPS->>TPS: Validar mensaje
    TPS->>SC: Obtener configuración del tópico
    SC-->>TPS: Configuración del endpoint

    TPS->>TPS: Crear TextMessage JCSMP
    TPS->>TPS: Configurar propiedades del mensaje
    TPS->>+SB: Publicar mensaje en tópico
    
    alt Publicación exitosa
        SB-->>-TPS: ACK
        TPS->>Log: Info: Mensaje publicado exitosamente
        TPS-->>-TC: MessageResponse(success=true)
        TC-->>-Client: 200 OK + MessageResponse
    else Error en publicación
        SB-->>TPS: NACK/Exception
        TPS->>Log: Error: Falló publicación
        TPS-->>TC: MessageResponse(success=false)
        TC-->>Client: 500 Error + MessageResponse
    end

    Note over SB,TCS: Consumo automático (si hay suscriptores)
    
    alt Hay suscriptores activos
        SB->>+TCS: Entregar mensaje
        TCS->>TCS: Procesar mensaje
        TCS->>TCS: Almacenar en consumedMessages
        TCS->>Log: Info: Mensaje consumido
        TCS-->>-SB: Procesamiento completo
    end
```

---

## 2. Flujo de Suscripción a Tópicos

### Escenario: Cliente se suscribe dinámicamente a un tópico

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant TC as Topic Controller
    participant TCS as Topic Consumer Service
    participant SC as Solace Config
    participant SB as Solace Broker

    Note over Client,SB: Flujo de Suscripción Dinámica

    Client->>+TC: POST /topics/{topicName}/subscribe
    TC->>+TCS: subscribeToTopic(topicName)

    TCS->>SC: Resolver nombre del tópico
    SC-->>TCS: Topic configurado

    TCS->>TCS: Crear Topic JCSMP
    TCS->>+SB: Añadir suscripción
    
    alt Suscripción exitosa
        SB-->>-TCS: Suscripción confirmada
        TCS->>TCS: Registrar suscripción activa
        TCS-->>-TC: Éxito
        TC-->>-Client: 200 OK
        
        Note over SB,TCS: A partir de ahora los mensajes<br/>se entregan automáticamente
        
    else Error en suscripción
        SB-->>TCS: Error
        TCS-->>TC: Exception
        TC-->>Client: 500 Error
    end
```

---

## 3. Flujo de Publicación en Colas con Reintentos

### Escenario: Cliente publica mensaje en cola con posibles fallos

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant QC as Queue Controller
    participant QPS as Queue Publisher Service
    participant RT as Retry Template
    participant SC as Solace Config
    participant SB as Solace Broker
    participant DLQ as Dead Letter Queue
    participant Log as Application Logs

    Note over Client,Log: Flujo de Publicación en Colas con Reintentos

    Client->>+QC: POST /queues/{queueName}/publish
    QC->>QC: Validar request
    QC->>+QPS: publishToQueue(queueName, message)

    QPS->>SC: Obtener configuración de cola
    SC-->>QPS: Queue endpoint

    QPS->>+RT: execute(publishOperation)
    
    loop Hasta 3 intentos máximo
        RT->>+QPS: Ejecutar intento
        QPS->>QPS: Crear mensaje persistente
        QPS->>+SB: Enviar mensaje a cola
        
        alt Intento exitoso
            SB-->>-QPS: ACK
            QPS->>Log: Info: Mensaje enviado exitosamente
            QPS-->>-RT: Éxito
            RT-->>-QPS: Operación completada
            QPS-->>-QC: MessageResponse(success=true)
            QC-->>-Client: 200 OK
        else Fallo temporal
            SB-->>QPS: NACK/Timeout
            QPS->>Log: Warn: Intento fallido, reintentando...
            QPS-->>RT: Exception
            RT->>RT: Esperar backoff exponencial
            Note right of RT: Backoff: 1s, 2s, 4s
        end
    end
    
    alt Todos los intentos fallaron
        RT-->>QPS: Max retries exceeded
        QPS->>+DLQ: sendToDeadLetterQueue(message, error)
        DLQ->>DLQ: Crear mensaje de error
        DLQ->>SB: Enviar a cola de error
        SB-->>-DLQ: ACK
        QPS->>Log: Error: Mensaje enviado a DLQ
        QPS-->>QC: MessageResponse(success=false)
        QC-->>Client: 500 Error
    end
```

---

## 4. Flujo de Consumo de Colas

### Escenario: Consumo automático de mensajes de cola

```mermaid
sequenceDiagram
    participant SB as Solace Broker
    participant QCS as Queue Consumer Service
    participant QueueEndpoint as Queue Endpoint
    participant MessageHandler as Message Handler
    participant Log as Application Logs
    participant Client as Cliente HTTP

    Note over SB,Client: Flujo de Consumo Automático de Colas

    SB->>+QCS: Mensaje disponible en cola
    QCS->>+QueueEndpoint: receiveMessage()
    
    QueueEndpoint->>+MessageHandler: onReceive(BytesXMLMessage)
    MessageHandler->>MessageHandler: Deserializar mensaje
    
    alt Procesamiento exitoso
        MessageHandler->>MessageHandler: Procesar lógica de negocio
        MessageHandler->>MessageHandler: Almacenar en consumedMessages
        MessageHandler->>Log: Info: Mensaje procesado exitosamente
        MessageHandler->>+SB: ACK mensaje
        SB-->>-MessageHandler: ACK confirmado
        MessageHandler-->>-QueueEndpoint: Éxito
        QueueEndpoint-->>-QCS: Mensaje procesado
        
    else Error en procesamiento
        MessageHandler->>Log: Error: Fallo en procesamiento
        MessageHandler->>+SB: NACK mensaje
        SB-->>-MessageHandler: NACK confirmado
        
        Note over SB: El broker reentregará<br/>el mensaje según configuración
        
        MessageHandler-->>QueueEndpoint: Error
        QueueEndpoint-->>QCS: Error procesamiento
    end

    Note over Client,Log: Consulta de mensajes procesados
    
    Client->>+QCS: GET /queues/{queueName}/consumed
    QCS->>QCS: Obtener mensajes de cache
    QCS-->>-Client: Lista de mensajes consumidos
```

---

## 5. Flujo Request-Response Completo

### Escenario: Comunicación request-response con correlation ID

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant RRC as Request-Response Controller
    participant RRS as Request-Response Service
    participant SC as Solace Config
    participant SB as Solace Broker
    participant ResponseHandler as Response Handler
    participant ExternalService as Servicio Externo
    participant Log as Application Logs

    Note over Client,Log: Flujo Request-Response Completo

    Client->>+RRC: POST /request-response/test
    Note right of Client: Body: Request payload

    RRC->>+RRS: sendRequestSync(message)
    
    RRS->>RRS: Generar correlationId único
    RRS->>RRS: Configurar replyTo topic
    RRS->>SC: Obtener configuración request topic
    SC-->>RRS: Topic de request configurado

    RRS->>+SB: Publicar request con correlationId
    Note right of RRS: Headers: correlationId, replyTo

    SB-->>-RRS: Request ACK
    RRS->>Log: Info: Request enviado, esperando respuesta...

    Note over SB,ExternalService: El request es procesado por servicio externo

    SB->>+ExternalService: Entregar request
    ExternalService->>ExternalService: Procesar request
    ExternalService->>ExternalService: Generar response
    ExternalService->>+SB: Publicar response en replyTo topic
    Note right of ExternalService: Headers: correlationId (mismo)
    SB-->>-ExternalService: Response ACK

    Note over SB,RRS: Response es entregado al solicitante

    SB->>+ResponseHandler: Entregar response
    ResponseHandler->>ResponseHandler: Validar correlationId
    
    alt CorrelationId matches
        ResponseHandler->>ResponseHandler: Correlacionar con request pendiente
        ResponseHandler->>+RRS: Notificar response recibido
        RRS->>RRS: Completar CompletableFuture
        RRS->>Log: Info: Response correlacionado exitosamente
        RRS-->>-RRC: Response object
        RRC-->>-Client: 200 OK + Response
    else CorrelationId no matches
        ResponseHandler->>Log: Warn: Response sin correlación
        ResponseHandler-->>-SB: Descartar mensaje
    end

    Note over RRS: Timeout handling (parallel)
    
    opt Timeout alcanzado
        RRS->>RRS: CompletableFuture timeout
        RRS->>Log: Warn: Request timeout
        RRS-->>RRC: TimeoutException
        RRC-->>Client: 408 Timeout
    end
```

---

## 6. Flujo de Manejo de Errores y Dead Letter Queue

### Escenario: Mensaje falla y es enviado a Dead Letter Queue

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant QPS as Queue Publisher Service
    participant RT as Retry Template
    participant SB as Solace Broker
    participant DLQ as Dead Letter Queue
    participant Admin as Administrador
    participant Log as Application Logs

    Note over Client,Log: Flujo de Manejo de Errores Completo

    Client->>+QPS: publishToQueue(message)
    QPS->>+RT: execute(publishOperation)

    loop 3 intentos con backoff exponencial
        RT->>QPS: Ejecutar intento
        QPS->>+SB: Enviar mensaje
        SB-->>-QPS: NACK/Exception
        QPS->>Log: Warn: Intento {attempt} fallido
        
        alt No es último intento
            RT->>RT: Calcular backoff delay
            Note right of RT: Delay = baseDelay * (2^attempt)
            RT->>RT: Thread.sleep(delay)
        end
    end

    RT-->>-QPS: Max retries exceeded

    QPS->>+DLQ: sendToDeadLetterQueue(originalMessage, errorInfo)
    
    DLQ->>DLQ: Crear SolaceMessage de error
    Note right of DLQ: Incluye mensaje original,<br/>stack trace, timestamp,<br/>número de reintentos
    
    DLQ->>DLQ: Configurar headers adicionales
    Note right of DLQ: error-reason, original-destination,<br/>retry-count, failure-timestamp
    
    DLQ->>+SB: Publicar en dead letter queue
    SB-->>-DLQ: ACK

    DLQ->>Log: Error: Mensaje enviado a DLQ
    DLQ-->>-QPS: DLQ operation completed
    
    QPS-->>-Client: 500 Internal Server Error

    Note over Admin,Log: Análisis posterior de errores

    Admin->>+SB: Consultar dead letter queue
    SB-->>-Admin: Mensajes de error disponibles
    
    Admin->>Admin: Analizar causa raíz
    
    alt Error corregido
        Admin->>SB: Republicar mensaje corregido
        Admin->>SB: Purgar mensaje de DLQ
    else Error persistente
        Admin->>Log: Registrar incidencia
        Admin->>Admin: Configurar alertas
    end
```

---

## 7. Flujo de Inicialización del Sistema

### Escenario: Startup y configuración de componentes

```mermaid
sequenceDiagram
    participant SpringBoot as Spring Boot
    participant SolaceConfig as Solace Config
    participant SolaceProps as Solace Properties
    participant SessionFactory as Session Factory
    participant TCS as Topic Consumer Service
    participant QCS as Queue Consumer Service
    participant SB as Solace Broker
    participant Log as Application Logs

    Note over SpringBoot,Log: Inicialización del Sistema

    SpringBoot->>+SolaceConfig: @Configuration init
    SolaceConfig->>+SolaceProps: Cargar propiedades
    SolaceProps->>SolaceProps: Validar configuración
    SolaceProps-->>-SolaceConfig: Propiedades cargadas

    SolaceConfig->>+SessionFactory: Crear JCSMPSession
    SessionFactory->>SessionFactory: Configurar connection factory
    SessionFactory->>+SB: Conectar al broker
    
    alt Conexión exitosa
        SB-->>-SessionFactory: Conexión establecida
        SessionFactory->>Log: Info: Conectado a Solace broker
        SessionFactory-->>-SolaceConfig: Session disponible
        
        SolaceConfig->>+TCS: @PostConstruct initializeTopicConsumer
        TCS->>TCS: Crear XMLMessageConsumer
        TCS->>TCS: Configurar MessageHandler
        TCS->>+SB: Suscribirse a tópicos configurados
        SB-->>-TCS: Suscripciones confirmadas
        TCS->>Log: Info: Suscripciones de tópicos activas
        TCS-->>-SolaceConfig: Topic consumer inicializado

        SolaceConfig->>+QCS: @PostConstruct initializeQueueConsumer  
        QCS->>QCS: Crear FlowReceiver endpoints
        QCS->>+SB: Conectar a colas configuradas
        SB-->>-QCS: Endpoints de cola activos
        QCS->>Log: Info: Consumidores de cola inicializados
        QCS-->>-SolaceConfig: Queue consumer inicializado

        SolaceConfig-->>-SpringBoot: Configuración completa
        
        SpringBoot->>Log: Info: Aplicación lista - puerto 8080
        SpringBoot->>SpringBoot: Exponer endpoints REST
        SpringBoot->>SpringBoot: Activar Actuator health checks
        
    else Error de conexión
        SB-->>SessionFactory: ConnectionException
        SessionFactory->>Log: Error: No se pudo conectar al broker
        SessionFactory-->>SolaceConfig: Exception
        SolaceConfig-->>SpringBoot: Bean initialization failed
        SpringBoot->>SpringBoot: Startup failed
    end
```

---

## 8. Flujo de Health Check y Monitoreo

### Escenario: Verificación del estado del sistema

```mermaid
sequenceDiagram
    participant Client as Cliente/Monitor
    participant Actuator as Spring Actuator
    participant HealthIndicator as Solace Health Indicator
    participant SB as Solace Broker
    participant TCS as Topic Consumer Service
    participant QCS as Queue Consumer Service

    Note over Client,QCS: Health Check y Monitoreo

    Client->>+Actuator: GET /actuator/health
    Actuator->>+HealthIndicator: checkHealth()
    
    par Verificar conexión broker
        HealthIndicator->>+SB: Ping/Check connection
        SB-->>-HealthIndicator: Connection status
    and Verificar topic consumers
        HealthIndicator->>+TCS: getConsumerStatus()
        TCS-->>-HealthIndicator: Consumer status
    and Verificar queue consumers  
        HealthIndicator->>+QCS: getConsumerStatus()
        QCS-->>-HealthIndicator: Consumer status
    end

    alt Todos los componentes saludables
        HealthIndicator->>HealthIndicator: status = UP
        HealthIndicator->>HealthIndicator: Agregar detalles
        HealthIndicator-->>-Actuator: HealthStatus.UP
        Actuator-->>-Client: 200 OK - Health UP
    else Algún componente no saludable
        HealthIndicator->>HealthIndicator: status = DOWN  
        HealthIndicator->>HealthIndicator: Agregar detalles de error
        HealthIndicator-->>Actuator: HealthStatus.DOWN
        Actuator-->>Client: 503 Service Unavailable - Health DOWN
    end

    Note over Client,QCS: Métricas adicionales

    Client->>+Actuator: GET /actuator/metrics
    Actuator->>Actuator: Recopilar métricas JVM
    Actuator->>TCS: Obtener métricas de tópicos
    TCS-->>Actuator: Mensajes procesados, errores, etc.
    Actuator->>QCS: Obtener métricas de colas
    QCS-->>Actuator: Mensajes procesados, errores, etc.
    Actuator-->>-Client: JSON con métricas completas
```

---

## Conclusión

Estos diagramas de secuencia proporcionan una visión detallada del funcionamiento interno del sistema Solace Client Archetype, mostrando:

- **Flujos de publicación/suscripción** en tópicos y colas
- **Manejo robusto de errores** con reintentos y Dead Letter Queue
- **Patrones request-response** con correlación de mensajes
- **Inicialización correcta** del sistema y sus componentes
- **Monitoreo y observabilidad** del estado de salud

Cada flujo está diseñado para ser resiliente, observable y fácil de troubleshoot en producción.