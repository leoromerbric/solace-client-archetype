# Guía de Inicio Rápido - Solace Client Archetype

## Instalación y Configuración

### 1. Prerrequisitos

- **Java 17+**: `java -version`
- **Maven 3.6+**: `mvn -version`
- **Broker Solace**: Instancia local o remota

### 2. Configuración del Broker Solace

#### Opción A: Solace PubSub+ Docker
```bash
docker run -d -p 8080:8080 -p 55555:55555 -p 8008:8008 -p 1883:1883 -p 8000:8000 -p 5672:5672 -p 9000:9000 -p 2222:2222 --shm-size=2g --env username_admin_globalaccesslevel=admin --env username_admin_password=admin --name=solace solace/solace-pubsub-standard
```

#### Opción B: Solace Cloud
Registrarse en [Solace Cloud](https://cloud.solace.com/) y obtener las credenciales.

### 3. Configuración de la Aplicación

Editar `src/main/resources/application.yml`:

```yaml
solace:
  client:
    broker:
      host: tcp://tu-broker-solace:55555
      vpn: tu-vpn
      username: tu-usuario
      password: tu-contraseña
```

O usar variables de entorno:
```bash
export SOLACE_HOST=tcp://tu-broker-solace:55555
export SOLACE_VPN=tu-vpn
export SOLACE_USERNAME=tu-usuario
export SOLACE_PASSWORD=tu-contraseña
```

### 4. Ejecutar la Aplicación

```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar aplicación
mvn spring-boot:run
```

## Verificación de Funcionamiento

### 1. Health Check
```bash
curl http://localhost:8080/api/v1/actuator/health
```

### 2. Documentación API
Abrir en navegador: `http://localhost:8080/api/v1/swagger-ui/index.html`

### 3. Test Básico de Tópicos

#### Suscribirse a un tópico
```bash
curl -X POST http://localhost:8080/api/v1/topics/test-topic/subscribe
```

#### Publicar mensaje
```bash
curl -X POST http://localhost:8080/api/v1/topics/test-topic/publish \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TEST",
    "payload": {
      "message": "¡Hola Solace!"
    }
  }'
```

#### Ver mensajes consumidos
```bash
curl http://localhost:8080/api/v1/topics/test-topic/consumed
```

### 4. Test Básico de Colas

#### Publicar orden
```bash
curl -X POST http://localhost:8080/api/v1/queues/orders/publish \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ORDER",
    "payload": {
      "orderId": "ORD-001",
      "amount": 100.50,
      "currency": "EUR"
    }
  }'
```

#### Ver mensajes procesados
```bash
curl http://localhost:8080/api/v1/queues/orders/consumed
```

### 5. Test Request-Response

#### Test simple
```bash
curl -X POST http://localhost:8080/api/v1/request-response/test \
  -H "Content-Type: application/json" \
  -d '{"testMessage": "Hello World"}'
```

## Colección de Postman

Importar el archivo `Solace_Client_Archetype_API.postman_collection.json` en Postman para acceder a todas las APIs con ejemplos preconfigurados.

### Variables de Postman
- `baseUrl`: `http://localhost:8080/api/v1`
- `topicName`: `test-topic`
- `queueName`: `test.queue`

## Estructura de Mensajes

### Mensaje Estándar
```json
{
  "id": "auto-generado-uuid",
  "type": "TIPO_MENSAJE",
  "payload": {
    "datos": "del mensaje"
  },
  "timestamp": "2023-10-15T10:30:00",
  "source": "solace-client-archetype",
  "destination": "nombre-destino",
  "headers": {
    "clave": "valor"
  },
  "correlationId": "id-correlacion"
}
```

### Respuesta Estándar
```json
{
  "success": true,
  "message": "Descripción de la operación",
  "messageId": "id-del-mensaje",
  "destination": "destino",
  "timestamp": "2023-10-15T10:30:00",
  "error": null
}
```

## Casos de Uso Comunes

### 1. Notificaciones en Tiempo Real
```bash
# Terminal 1: Suscribirse
curl -X POST http://localhost:8080/api/v1/topics/notifications/subscribe

# Terminal 2: Publicar notificación
curl -X POST http://localhost:8080/api/v1/topics/notifications/publish \
  -H "Content-Type: application/json" \
  -d '{
    "type": "NOTIFICATION",
    "payload": {
      "title": "Nueva Orden",
      "body": "Se ha recibido una nueva orden #12345",
      "priority": "HIGH"
    }
  }'

# Terminal 1: Ver mensajes recibidos
curl http://localhost:8080/api/v1/topics/notifications/consumed
```

### 2. Procesamiento de Órdenes
```bash
# Enviar orden a procesar
curl -X POST http://localhost:8080/api/v1/queues/orders/publish \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ORDER",
    "payload": {
      "orderId": "ORD-12345",
      "customerId": "CUST-789",
      "items": [
        {"productId": "PROD-001", "quantity": 2, "price": 25.99}
      ],
      "total": 51.98
    }
  }'

# Verificar procesamiento
curl http://localhost:8080/api/v1/queues/orders/stats
```

### 3. Consultas Síncronas
```bash
curl -X POST http://localhost:8080/api/v1/request-response/send-sync \
  -H "Content-Type: application/json" \
  -d '{
    "type": "QUERY",
    "payload": {
      "operation": "getUserProfile",
      "userId": "user123"
    }
  }'
```

## Troubleshooting

### Error de Conexión a Solace
```
Error: JCSMPTransportException
```
**Solución**: Verificar que el broker Solace esté ejecutándose y la configuración de conexión sea correcta.

### Puerto ya en uso
```
Error: Port 8080 was already in use
```
**Solución**: Cambiar el puerto en `application.yml` o detener el proceso que usa el puerto.

### Permisos de Cola/Tópico
```
Error: Permission denied
```
**Solución**: Verificar que el usuario tenga permisos para las colas y tópicos configurados.

## Monitoreo

### Métricas Disponibles
```bash
# Ver todas las métricas
curl http://localhost:8080/api/v1/actuator/metrics

# Métrica específica - memoria JVM
curl http://localhost:8080/api/v1/actuator/metrics/jvm.memory.used

# Estadísticas Request-Response
curl http://localhost:8080/api/v1/request-response/stats
```

### Logs
Los logs se guardan en `logs/solace-client.log` y también aparecen en la consola.

Niveles de log configurables en `application.yml`:
```yaml
logging:
  level:
    com.solace.client.archetype: DEBUG  # Para debugging detallado
```

## Personalización

### Añadir Nuevos Tipos de Mensaje
1. Crear clase en `src/main/java/com/solace/client/archetype/model/`
2. Actualizar servicios de procesamiento
3. Añadir endpoints específicos si es necesario

### Configurar Nuevas Colas
1. Añadir en `application.yml`:
```yaml
solace:
  client:
    queues:
      mi-nueva-cola: mi.nueva.cola
```
2. Actualizar `SolaceProperties.java`
3. Añadir métodos en los servicios correspondientes

### Personalizar Manejo de Errores
Editar `GlobalExceptionHandler.java` para manejar excepciones específicas.

## Despliegue

### Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/solace-client-archetype-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
mvn clean package
docker build -t solace-client-archetype .
docker run -p 8080:8080 \
  -e SOLACE_HOST=tcp://broker:55555 \
  solace-client-archetype
```

### Kubernetes
Ver ejemplos de configuración en la documentación avanzada del proyecto.

## Soporte

- **Documentación**: [README.md](README.md)
- **Issues**: [GitHub Issues](https://github.com/leoromerbric/solace-client-archetype/issues)
- **API Docs**: `http://localhost:8080/api/v1/swagger-ui/index.html`