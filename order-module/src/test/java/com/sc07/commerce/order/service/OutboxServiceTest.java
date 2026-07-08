package com.sc07.commerce.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.order.entity.OutboxEvent;
import com.sc07.commerce.order.repository.OutboxEventRepository;
import com.sc07.commerce.shared.v1.OrderEvent;
import com.sc07.commerce.shared.v1.OrderEventType;
import com.sc07.commerce.shared.v1.OrderEvents;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TraceService traceService;

    @Mock
    private Tracer tracer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void publishPendingPublishesEventWithTenantHeaderAndMarksRowPublished() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrderEvent event = new OrderEvent(
                eventId,
                OrderEventType.ORDER_CREATED,
                OrderEvent.CURRENT_VERSION,
                tenantId,
                Instant.parse("2026-07-08T00:00:00Z"),
                new OrderEvent.Payload(orderId, null, "buyer@example.com", null, "PENDING")
        );
        OutboxEvent row = new OutboxEvent(
                eventId,
                tenantId.toString(),
                OrderEventType.ORDER_CREATED.name(),
                objectMapper.writeValueAsString(event),
                null,
                Instant.parse("2026-07-08T00:00:00Z"),
                null
        );
        Span span = mock(Span.class);
        Tracer.SpanInScope scope = mock(Tracer.SpanInScope.class);
        when(outboxEventRepository.findPending(PageRequest.of(0, 50))).thenReturn(List.of(row));
        when(traceService.startSpan("Publish ORDER_CREATED")).thenReturn(span);
        when(tracer.withSpan(span)).thenReturn(scope);

        new OutboxService(outboxEventRepository, rabbitTemplate, objectMapper, traceService, tracer)
                .publishPending();

        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(OrderEvents.EXCHANGE),
                eq("order.created"),
                eq(event),
                processorCaptor.capture()
        );

        Message message = new Message(new byte[0], new MessageProperties());
        processorCaptor.getValue().postProcessMessage(message);
        Object tenantHeader = message.getMessageProperties().getHeader(OrderEvents.TENANT_HEADER);
        assertThat(tenantHeader)
                .isEqualTo(tenantId.toString());
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
        assertThat(row.getPublishedAt()).isNotNull();
        verify(span).end();
        verify(scope).close();
    }
}
