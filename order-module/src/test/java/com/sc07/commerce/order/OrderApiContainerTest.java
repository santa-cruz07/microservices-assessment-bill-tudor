package com.sc07.commerce.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.order.configuration.ApiKeyTenantFilter;
import com.sc07.commerce.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
        "otel.sdk.disabled=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
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

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void createsReadsAndTransitionsOrder() throws Exception {
        String id = createOrder("buyer@example.com", "Laptop", 1);

        assertOrder(id, "buyer@example.com", "Laptop", 1, OrderStatus.PENDING);

        updateStatus(id, OrderStatus.PROCESSING)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.PROCESSING.name()));

        cancelOrder(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name()));
    }

    @Test
    void enforcesApiKeyAndTenantIsolation() throws Exception {
        createOrder("tenant-one@example.com", "Keyboard", 1);

        // Missing API key should be rejected.
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        // Tenant 2 should not see tenant 1's order.
        mockMvc.perform(get("/api/orders")
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-2-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Tenant 1 should see its own order.
        mockMvc.perform(get("/api/orders")
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-1-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Lower-tier tests worth adding later:
        // - invalid API key returns 401
        // - invalid create payload returns 400
        // - unknown order id returns 404
        // - illegal status transition returns 409
    }

    private String createOrder(String email, String itemDescription, int quantity) throws Exception {
        String responseBody = mockMvc.perform(post("/api/orders")
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-1-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerEmail": "%s",
                                  "itemDescription": "%s",
                                  "quantity": %d
                                }
                                """.formatted(email, itemDescription, quantity)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/orders/")))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("id").asText();
    }

    private void assertOrder(
            String id,
            String email,
            String itemDescription,
            int quantity,
            OrderStatus status
    ) throws Exception {
        mockMvc.perform(get("/api/orders/{id}", id)
                        .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-1-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.customerEmail").value(email))
                .andExpect(jsonPath("$.itemDescription").value(itemDescription))
                .andExpect(jsonPath("$.quantity").value(quantity))
                .andExpect(jsonPath("$.status").value(status.name()));
    }

    private ResultActions updateStatus(String id, OrderStatus status) throws Exception {
        return mockMvc.perform(post("/api/orders/{id}/status", id)
                .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-1-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s"}
                        """.formatted(status.name())));
    }

    private ResultActions cancelOrder(String id) throws Exception {
        return mockMvc.perform(post("/api/orders/{id}/cancel", id)
                .header(ApiKeyTenantFilter.API_KEY_HEADER, "company-1-key"));
    }
}