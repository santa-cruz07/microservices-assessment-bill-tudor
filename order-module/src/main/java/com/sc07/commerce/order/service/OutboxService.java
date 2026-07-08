package com.sc07.commerce.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.order.entity.OutboxEvent;
import com.sc07.commerce.order.repository.OutboxEventRepository;
import com.sc07.commerce.shared.v1.OrderEvent;
import com.sc07.commerce.shared.v1.OrderEvents;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class OutboxService {


    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TraceService traceService;
    private final Tracer tracer;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            TraceService traceService,
            Tracer tracer) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.traceService = traceService;
        this.tracer = tracer;
    }


    @Scheduled(fixedDelayString = "${outbox.poll-rate-ms:1000}")
    @Transactional
    public void publishPending() {

        List<OutboxEvent> events =
                outboxEventRepository.findPending(PageRequest.of(0, 50));

        for (OutboxEvent row : events) {
            boolean published = publishEvent(row);
            if (!published) {
                break;
            }
        }
    }

    private boolean publishEvent(OutboxEvent row) {
        Span span = traceService.startSpan("Publish " + row.getEventType());

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            OrderEvent event = objectMapper.readValue(row.getPayload(), OrderEvent.class);

            MessagePostProcessor messagePostProcessor = message -> {
                message.getMessageProperties()
                        .setHeader(OrderEvents.TENANT_HEADER, row.getTenantId());

                message.getMessageProperties()
                        .setMessageId(row.getId().toString());

                return message;
            };

            rabbitTemplate.convertAndSend(
                    OrderEvents.EXCHANGE,
                    OrderEvents.routingKey(event.eventType()),
                    event,
                    messagePostProcessor
            );

            row.markPublished();
            return true;

        } catch (Exception ex) {
            span.error(ex);
            log.warn("Failed to publish outbox event {}", row.getId(), ex);
            return false;

        } finally {
            span.end();
        }
    }
}

