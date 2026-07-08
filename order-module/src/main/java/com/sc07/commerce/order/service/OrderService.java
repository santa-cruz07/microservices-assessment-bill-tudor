package com.sc07.commerce.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.order.configuration.TenantPrincipal;
import com.sc07.commerce.order.entity.Order;
import com.sc07.commerce.order.entity.OrderStatus;
import com.sc07.commerce.order.entity.OutboxEvent;
import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.exception.OrderNotFoundException;
import com.sc07.commerce.order.repository.OutboxEventRepository;
import com.sc07.commerce.order.repository.OrderRepository;
import com.sc07.commerce.shared.v1.OrderEvent;
import com.sc07.commerce.shared.v1.OrderEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TraceService traceService;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            TraceService traceService) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.traceService = traceService;
    }

    @Transactional
    public Order create(@Valid OrderCreate request) {
        Order order = orderRepository.save(new Order(request));
        appendOutbox(order, OrderEventType.ORDER_CREATED, null, order.getStatus());
        return order;
    }

    @Transactional(readOnly = true)
    public Order getById(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    //Realistically this would have pagination passed to it.
    @Transactional(readOnly = true)
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order changeStatus(UUID id, @NotNull OrderStatus status) {
        Order order = this.getById(id);
        OrderStatus previousStatus = order.updateStatus(status);
        log.debug("Order status changed from {} to {}", previousStatus, status);

        Order result = orderRepository.save(order);
        OrderEventType eventType = status == OrderStatus.CANCELLED
                ? OrderEventType.ORDER_CANCELLED
                : OrderEventType.ORDER_STATUS_CHANGED;
        appendOutbox(result, eventType, previousStatus, status);
        return result;

    }

    @Transactional
    public Order cancel(UUID id) {
        return this.changeStatus(id, OrderStatus.CANCELLED);
    }

    private void appendOutbox(
            Order order,
            OrderEventType eventType,
            OrderStatus previousStatus,
            OrderStatus newStatus) {
        UUID tenantId = currentTenantId();
        OrderEvent event = new OrderEvent(
                UUID.randomUUID(),
                eventType,
                OrderEvent.CURRENT_VERSION,
                tenantId,
                Instant.now(),
                new OrderEvent.Payload(
                        order.getId(),
                        null,
                        order.getCustomerEmail(),
                        previousStatus == null ? null : previousStatus.name(),
                        newStatus.name()));

        outboxEventRepository.save(new OutboxEvent(
                null,
                tenantId.toString(),
                eventType.name(),
                writePayload(event),
                traceService.currentTraceId().orElse(null),
                event.occurredAt(),
                null));
    }

    private UUID currentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof TenantPrincipal principal)) {
            throw new IllegalStateException("No tenant principal found");
        }

        return principal.tenantId();
    }

    private String writePayload(OrderEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize order event", ex);
        }
    }

}
