package com.sc07.commerce.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.order.configuration.ApiKeyTenantFilter;
import com.sc07.commerce.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "management.otlp.tracing.endpoint=http://localhost:9/v1/traces",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "outbox.poll-rate-ms=600000"
})
@AutoConfigureMockMvc
class OrderApiContainerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders")
            .withUsername("app")
            .withPassword("app");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void apiKeyScopesOrdersByTenant() throws Exception {
        createOrder("tenant-one@example.com", "Keyboard", "company-1-key");

        mockMvc.perform(get("/api/orders")
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-1-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerEmail").value("tenant-one@example.com"));

        mockMvc.perform(get("/api/orders")
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-2-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String createOrder(String email, String itemDescription, String apiKey) throws Exception {
        String responseBody = mockMvc.perform(post("/api/orders")
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerEmail": "%s",
                                  "itemDescription": "%s",
                                  "quantity": 1
                                }
                                """.formatted(email, itemDescription)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/orders/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("id").asText();
    }
}
