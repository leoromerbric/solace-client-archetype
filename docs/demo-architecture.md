# Arquitectura de la Demo - Solace Client Archetype

## Introducción

Este documento describe la arquitectura específica de la demostración del Solace Client Archetype, incluyendo casos de uso concretos, configuración de demo y ejemplos de integración.

---

## Arquitectura de la Demo

### Componentes de la Demo

```mermaid
graph TB
    subgraph "Entorno de Demostración"
        subgraph "Cliente Demo"
            PostmanCollection[Postman Collection]
            CurlScripts[Scripts cURL]
            WebUI[Swagger UI]
            TestClient[Cliente de Test]
        end

        subgraph "Solace Client Archetype (Microservicio)"
            subgraph "APIs de Demo"
                NotificationAPI[Notifications API]
                OrderAPI[Orders API]
                EventAPI[Events API]
                RequestResponseAPI[Request-Response API]
            end

            subgraph "Servicios de Demo"
                NotificationService[Notification Service]
                OrderService[Order Service]
                EventService[Event Service]
            end
        end

        subgraph "Solace PubSub+ Broker"
            subgraph "Topics de Demo"
                NotificationTopic[solace/notifications]
                EventTopic[solace/events]
                TestTopic[solace/test]
            end

            subgraph "Queues de Demo"
                OrderQueue[solace.queue.orders]
                NotificationQueue[solace.queue.notifications]
                DeadLetterQueue[solace.queue.deadletter]
            end

            subgraph "Request-Response"
                RequestTopic[solace/request/test]
                ReplyTopic[solace/reply/test]
            end
        end

        subgraph "Monitoreo Demo"
            SolaceManager[Solace PubSub+ Manager]
            ActuatorEndpoints[Actuator Metrics]
            AppLogs[Application Logs]
        end
    end

    %% Conexiones de Demo
    PostmanCollection --> NotificationAPI
    PostmanCollection --> OrderAPI
    PostmanCollection --> EventAPI
    PostmanCollection --> RequestResponseAPI

    CurlScripts --> NotificationAPI
    CurlScripts --> OrderAPI
    WebUI --> RequestResponseAPI

    NotificationAPI --> NotificationService
    OrderAPI --> OrderService
    EventAPI --> EventService

    NotificationService --> NotificationTopic
    NotificationService --> NotificationQueue

    OrderService --> OrderQueue
    OrderService --> DeadLetterQueue

    EventService --> EventTopic

    RequestResponseAPI --> RequestTopic
    RequestTopic --> ReplyTopic
    ReplyTopic --> RequestResponseAPI

    %% Monitoreo
    SolaceManager --> NotificationTopic
    SolaceManager --> OrderQueue
    ActuatorEndpoints --> AppLogs

    %% Estilos
    classDef clientClass fill:#e3f2fd
    classDef apiClass fill:#f3e5f5
    classDef serviceClass fill:#e8f5e8
    classDef brokerClass fill:#fff3e0
    classDef monitorClass fill:#f1f8e9

    class PostmanCollection,CurlScripts,WebUI,TestClient clientClass
    class NotificationAPI,OrderAPI,EventAPI,RequestResponseAPI apiClass
    class NotificationService,OrderService,EventService serviceClass
    class NotificationTopic,EventTopic,OrderQueue,NotificationQueue,DeadLetterQueue,RequestTopic,ReplyTopic brokerClass
    class SolaceManager,ActuatorEndpoints,AppLogs monitorClass
```

---

## Casos de Uso de la Demo

### 1. Caso de Uso: Sistema de Notificaciones

**Descripción**: Demostración del patrón pub/sub para notificaciones del sistema.

#### Escenario de Demo

```mermaid
sequenceDiagram
    participant Demo as Demo Client
    participant API as Notifications API
    participant Service as Notification Service
    participant Broker as Solace Broker
    participant Consumer as Notification Consumer
    participant UI as Demo Dashboard

    Note over Demo,UI: Demo: Sistema de Notificaciones

    Demo->>API: POST /topics/notifications/publish
    Note right of Demo: {<br/>  "type": "USER_WELCOME",<br/>  "payload": {<br/>    "userId": "demo-123",<br/>    "message": "Bienvenido!"<br/>  }<br/>}

    API->>Service: publishNotification(message)
    Service->>Broker: Publicar en solace/notifications
    Broker-->>Service: ACK

    Note over Broker,Consumer: Consumo automático
    Broker->>Consumer: Entregar notificación
    Consumer->>UI: Actualizar dashboard
    
    Service-->>API: Success response
    API-->>Demo: 200 OK

    Demo->>API: GET /topics/notifications/consumed
    API-->>Demo: Lista de notificaciones procesadas
```

#### Configuración de Demo
```yaml
# application.yml - Demo Configuration
solace:
  client:
    topics:
      notifications: "solace/demo/notifications"
    demo:
      notification-types:
        - USER_WELCOME
        - SYSTEM_ALERT
        - ORDER_CONFIRMATION
```

---

### 2. Caso de Uso: Procesamiento de Órdenes

**Descripción**: Demostración del patrón de colas para procesamiento garantizado de órdenes.

#### Escenario de Demo

```mermaid
sequenceDiagram
    participant Demo as Demo Client
    participant API as Orders API
    participant Service as Order Service
    participant Broker as Solace Broker
    participant Consumer as Order Consumer
    participant DLQ as Dead Letter Queue
    participant Monitor as Demo Monitor

    Note over Demo,Monitor: Demo: Procesamiento de Órdenes

    Demo->>API: POST /queues/orders/publish
    Note right of Demo: {<br/>  "type": "ORDER",<br/>  "payload": {<br/>    "orderId": "ORD-DEMO-001",<br/>    "amount": 99.99,<br/>    "currency": "EUR"<br/>  }<br/>}

    API->>Service: processOrder(orderMessage)
    
    alt Orden válida
        Service->>Broker: Enviar a solace.queue.orders
        Broker->>Consumer: Procesar orden
        Consumer->>Consumer: Validar y procesar
        Consumer->>Broker: ACK
        Consumer->>Monitor: Actualizar estadísticas
        Service-->>API: Order processed successfully
        API-->>Demo: 200 OK
    else Orden inválida (simulación de error)
        Service->>Service: Simular fallo de validación
        Service->>DLQ: Enviar a dead letter queue
        Service-->>API: Order processing failed
        API-->>Demo: 500 Error
    end

    Demo->>API: GET /queues/orders/stats
    API-->>Demo: Estadísticas de procesamiento

    Demo->>Monitor: Consultar DLQ
    Monitor-->>Demo: Mensajes en cola de error
```

#### Tipos de Órdenes de Demo
```json
// Ejemplos de órdenes para testing
{
  "validOrder": {
    "type": "ORDER",
    "payload": {
      "orderId": "ORD-DEMO-001",
      "customerId": "CUST-123",
      "amount": 99.99,
      "currency": "EUR",
      "items": ["ITEM-A", "ITEM-B"]
    }
  },
  "invalidOrder": {
    "type": "ORDER",
    "payload": {
      "orderId": "INVALID-ORDER",
      "amount": -10.00  // Monto negativo para simular error
    }
  }
}
```

---

### 3. Caso de Uso: Request-Response en Tiempo Real

**Descripción**: Demostración de comunicación síncrona con correlation ID.

#### Escenario de Demo

```mermaid
sequenceDiagram
    participant Demo as Demo Client
    participant API as Request-Response API
    participant Service as RR Service
    participant Broker as Solace Broker
    participant MockService as Mock Response Service
    participant Correlator as Correlation Handler

    Note over Demo,Correlator: Demo: Request-Response

    Demo->>API: POST /request-response/test
    Note right of Demo: {<br/>  "operation": "DEMO_CALC",<br/>  "data": {<br/>    "x": 10,<br/>    "y": 5<br/>  }<br/>}

    API->>Service: sendRequestSync(demoRequest)
    Service->>Service: Generar correlationId: "DEMO-123"
    Service->>Broker: Publicar en solace/request/test
    Note right of Service: Headers: correlationId, replyTo

    Broker->>MockService: Entregar request
    MockService->>MockService: Procesar demo calculation
    Note right of MockService: result = x + y = 15

    MockService->>Broker: Publicar response
    Note left of MockService: Headers: correlationId: "DEMO-123"

    Broker->>Correlator: Entregar response
    Correlator->>Service: Correlacionar response
    Service-->>API: Demo calculation result
    API-->>Demo: 200 OK + Result

    Note over Demo: {<br/>  "success": true,<br/>  "message": "Demo completado",<br/>  "request": {...},<br/>  "response": {<br/>    "result": 15,<br/>    "operation": "ADD"<br/>  }<br/>}
```

---

## Configuración Específica de Demo

### Docker Compose para Demo

```yaml
# docker-compose.demo.yml
version: '3.8'

services:
  solace-broker:
    image: solace/solace-pubsub-standard:latest
    container_name: solace-demo-broker
    ports:
      - "8080:8080"   # Solace Manager
      - "55555:55555" # SMF
      - "1943:1943"   # SEMP over TLS
      - "8008:8008"   # Web transport
    environment:
      - username_admin_globalaccesslevel=admin
      - username_admin_password=admin
      - system_scaling_maxconnectioncount=100
    volumes:
      - solace-demo-data:/var/lib/solace

  solace-client-demo:
    build: .
    container_name: solace-client-demo
    ports:
      - "8081:8080"
    environment:
      - SOLACE_HOST=tcp://solace-broker:55555
      - SOLACE_VPN=default
      - SOLACE_USERNAME=demo-client
      - SOLACE_PASSWORD=demo-password
      - SPRING_PROFILES_ACTIVE=demo
    depends_on:
      - solace-broker

volumes:
  solace-demo-data:
```

### Perfil de Configuración Demo

```yaml
# application-demo.yml
spring:
  application:
    name: solace-client-archetype-demo

solace:
  client:
    broker:
      host: tcp://localhost:55555
      vpn: default
      username: demo-client
      password: demo-password
    
    topics:
      notifications: "demo/notifications"
      events: "demo/events"
      test: "demo/test"
    
    queues:
      orders: "demo.queue.orders"
      notifications: "demo.queue.notifications"
      dead-letter: "demo.queue.deadletter"
    
    request-response:
      request-topic: "demo/request/test"
      timeout-ms: 5000
    
    retry:
      max-attempts: 2  # Menos reintentos para demo
      backoff-delay: 1000
      multiplier: 2.0

logging:
  level:
    com.solace.client.archetype: DEBUG
  pattern:
    console: "[DEMO] %d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

---

## Scripts de Demo

### Script de Inicialización de Demo

```bash
#!/bin/bash
# demo-setup.sh

echo "🚀 Configurando Demo de Solace Client Archetype..."

# 1. Levantar infraestructura
echo "📦 Iniciando contenedores..."
docker-compose -f docker-compose.demo.yml up -d

# 2. Esperar a que Solace esté listo
echo "⏳ Esperando a que Solace Broker esté listo..."
sleep 30

# 3. Configurar colas y tópicos de demo
echo "🔧 Configurando objetos de mensajería..."
curl -X POST http://admin:admin@localhost:8080/SEMP/v2/config/msgVpns/default/queues \
  -H "Content-Type: application/json" \
  -d '{"queueName": "demo.queue.orders", "accessType": "exclusive", "permission": "consume"}'

# 4. Iniciar aplicación demo
echo "🎯 Iniciando aplicación demo..."
java -jar target/solace-client-archetype-1.0.0.jar --spring.profiles.active=demo

echo "✅ Demo configurado correctamente!"
echo "🌐 Swagger UI: http://localhost:8081/api/v1/swagger-ui/index.html"
echo "🎛️ Solace Manager: http://localhost:8080 (admin/admin)"
```

### Script de Test de Demo

```bash
#!/bin/bash
# demo-test.sh

BASE_URL="http://localhost:8081/api/v1"

echo "🧪 Ejecutando tests de demo..."

# Test 1: Health Check
echo "1️⃣ Health Check..."
curl -s "$BASE_URL/actuator/health" | jq '.'

# Test 2: Notificación
echo "2️⃣ Enviando notificación demo..."
curl -X POST "$BASE_URL/topics/notifications/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DEMO_NOTIFICATION",
    "payload": {
      "message": "¡Hola desde la demo!",
      "timestamp": "'$(date -Iseconds)'"
    }
  }' | jq '.'

# Test 3: Orden
echo "3️⃣ Procesando orden demo..."
curl -X POST "$BASE_URL/queues/orders/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ORDER",
    "payload": {
      "orderId": "DEMO-'$(date +%s)'",
      "amount": 49.99,
      "currency": "EUR"
    }
  }' | jq '.'

# Test 4: Request-Response
echo "4️⃣ Test Request-Response..."
curl -X POST "$BASE_URL/request-response/test" \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "DEMO_TEST",
    "data": {
      "message": "Hello World Demo"
    }
  }' | jq '.'

# Test 5: Consultar mensajes consumidos
echo "5️⃣ Consultando mensajes procesados..."
curl -s "$BASE_URL/topics/notifications/consumed" | jq '.'

echo "✅ Tests de demo completados!"
```

---

## Casos de Prueba de la Demo

### Caso de Prueba 1: Flujo Exitoso Completo

```json
{
  "name": "Demo Flow - Happy Path",
  "description": "Prueba el flujo completo de la demo sin errores",
  "steps": [
    {
      "step": 1,
      "action": "POST /topics/notifications/publish",
      "payload": {
        "type": "DEMO_START",
        "payload": {
          "sessionId": "demo-session-001",
          "timestamp": "2024-01-15T10:00:00Z"
        }
      },
      "expectedResponse": 200
    },
    {
      "step": 2,
      "action": "POST /queues/orders/publish",
      "payload": {
        "type": "ORDER",
        "payload": {
          "orderId": "DEMO-ORDER-001",
          "amount": 99.99,
          "currency": "EUR"
        }
      },
      "expectedResponse": 200
    },
    {
      "step": 3,
      "action": "POST /request-response/test",
      "payload": {
        "testMessage": "Demo Request-Response"
      },
      "expectedResponse": 200
    },
    {
      "step": 4,
      "action": "GET /actuator/health",
      "expectedResponse": 200,
      "expectedStatus": "UP"
    }
  ]
}
```

### Caso de Prueba 2: Manejo de Errores

```json
{
  "name": "Demo Error Handling",
  "description": "Prueba el manejo de errores y Dead Letter Queue",
  "steps": [
    {
      "step": 1,
      "action": "POST /queues/orders/publish",
      "payload": {
        "type": "ORDER",
        "payload": {
          "orderId": "INVALID-ORDER",
          "amount": -100.00,
          "currency": "INVALID"
        }
      },
      "expectedResponse": 500,
      "description": "Orden inválida debe generar error"
    },
    {
      "step": 2,
      "action": "GET /queues/deadletter/consumed",
      "expectedResponse": 200,
      "description": "Verificar mensaje en DLQ"
    }
  ]
}
```

---

## Métricas y Monitoreo de la Demo

### Dashboard de Demo

```mermaid
graph TB
    subgraph "Demo Monitoring Dashboard"
        subgraph "Métricas de Rendimiento"
            TPS[Messages/Second]
            Latency[Average Latency]
            ErrorRate[Error Rate %]
        end

        subgraph "Estado de Componentes"
            BrokerStatus[Broker Connection]
            ConsumerStatus[Consumer Status]
            QueueDepth[Queue Depth]
        end

        subgraph "Demo Statistics"
            NotificationsSent[Notifications Sent]
            OrdersProcessed[Orders Processed]
            RequestsCompleted[RR Requests Completed]
            ErrorsInDLQ[Messages in DLQ]
        end
    end

    TPS --> ActuatorMetrics[Spring Actuator]
    Latency --> ActuatorMetrics
    ErrorRate --> ActuatorMetrics
    
    BrokerStatus --> SolaceHealth[Solace Health Check]
    ConsumerStatus --> SolaceHealth
    QueueDepth --> SolaceHealth
    
    NotificationsSent --> DemoCounters[Demo Counters]
    OrdersProcessed --> DemoCounters
    RequestsCompleted --> DemoCounters
    ErrorsInDLQ --> DemoCounters
```

### Endpoints de Monitoreo de Demo

```bash
# Métricas específicas de demo
GET /actuator/metrics/demo.notifications.sent
GET /actuator/metrics/demo.orders.processed
GET /actuator/metrics/demo.requests.completed

# Health checks específicos
GET /actuator/health/solace
GET /actuator/health/demo-components

# Info de la demo
GET /actuator/info
```

---

## Conclusión

La arquitectura de demo del Solace Client Archetype proporciona:

1. **Casos de uso realistas** que demuestran los patrones de mensajería empresarial
2. **Configuración simplificada** para facilitar las pruebas
3. **Scripts automatizados** para configuración y testing
4. **Monitoreo visual** del comportamiento del sistema
5. **Casos de prueba documentados** para validación

Esta demo permite a los usuarios entender rápidamente las capacidades del sistema y experimentar con diferentes escenarios de mensajería de forma interactiva.