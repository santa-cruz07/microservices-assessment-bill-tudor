package com.sc07.commerce.order.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceServiceTest {

    private final Tracer tracer = mock(Tracer.class);
    private final TraceService traceService = new TraceService(tracer);

    @Test
    void currentTraceIdIsEmptyWhenNoSpanIsActive() {
        assertThat(traceService.currentTraceId()).isEmpty();
    }

    @Test
    void currentTraceIdReturnsActiveSpanTraceId() {
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("trace-123");

        assertThat(traceService.currentTraceId()).contains("trace-123");
    }

    @Test
    void startSpanNamesAndStartsNextSpan() {
        Span span = mock(Span.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name("publish")).thenReturn(span);
        when(span.start()).thenReturn(span);

        assertThat(traceService.startSpan("publish")).isSameAs(span);
    }
}
