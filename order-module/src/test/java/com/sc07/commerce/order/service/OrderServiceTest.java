package com.sc07.commerce.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.order.configuration.ApiKeyAuthToken;
import com.sc07.commerce.order.configuration.TenantPrincipal;
import com.sc07.commerce.order.entity.Order;
import com.sc07.commerce.order.entity.OrderStatus;
import com.sc07.commerce.order.entity.OutboxEvent;
import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.exception.OrderNotFoundException;
import com.sc07.commerce.order.repository.OutboxEventRepository;
import com.sc07.commerce.order.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID TENANT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TraceService traceService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setTenant() {
        SecurityContextHolder.getContext()
                .setAuthentication(new ApiKeyAuthToken(new TenantPrincipal(TENANT_ID)));
    }

    @AfterEach
    void clearTenant() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBuildsPendingOrderAndPersistsIt() throws Exception {
        Order saved = new Order(new OrderCreate("buyer@example.com", "Laptop", 1));
        saved.setId(UUID.randomUUID());
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(traceService.currentTraceId()).thenReturn(Optional.empty());

        Order result = orderService.create(new OrderCreate("buyer@example.com", "Laptop", 1));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(captor.getValue().getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(result).isSameAs(saved);
        verifyOutbox("ORDER_CREATED", null);
    }


    @Test
    void getByIdThrowsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void changeStatusLoadsMutatesAndSavesOrder() throws Exception {
        UUID id = UUID.randomUUID();
        Order order = new Order(new OrderCreate("buyer@example.com", "Laptop", 1));
        order.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(traceService.currentTraceId()).thenReturn(Optional.empty());

        Order result = orderService.changeStatus(id, OrderStatus.PROCESSING);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(order);
        verifyOutbox("ORDER_STATUS_CHANGED", null);
    }

    @Test
    void cancelTransitionsOrderToCancelled() throws Exception {
        UUID id = UUID.randomUUID();
        Order order = new Order(new OrderCreate("buyer@example.com", "Laptop", 1));
        order.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(traceService.currentTraceId()).thenReturn(Optional.empty());

        assertThat(orderService.cancel(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verifyOutbox("ORDER_CANCELLED", null);
    }

    private void verifyOutbox(String eventType, String traceId) {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID.toString());
        assertThat(captor.getValue().getEventType()).isEqualTo(eventType);
        assertThat(captor.getValue().getPayload()).isEqualTo("{}");
        assertThat(captor.getValue().getTraceparent()).isEqualTo(traceId);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }
}
