package com.solace.client.archetype;

import com.solace.client.archetype.service.QueueConsumerService;
import com.solace.client.archetype.service.TopicConsumerService;
import com.solace.client.archetype.service.RequestResponseService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * Test básico para verificar que el contexto de Spring Boot se carga correctamente.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "solace.client.broker.host=tcp://localhost:55555",
    "solace.client.broker.vpn=default", 
    "solace.client.broker.username=test",
    "solace.client.broker.password=test"
})
class SolaceClientArchetypeApplicationTests {

    @MockBean
    private JCSMPSession jcsmpSession;
    
    @MockBean
    private XMLMessageProducer messageProducer;
    
    // Mock de los servicios que requieren inicialización automática
    @MockBean
    private QueueConsumerService queueConsumerService;
    
    @MockBean
    private TopicConsumerService topicConsumerService;
    
    @MockBean
    private RequestResponseService requestResponseService;

    @Test
    void contextLoads() {
        // Este test verifica que el contexto de Spring Boot se carga correctamente
        // Usando mocks para evitar dependencias externas en tests
    }
}