package com.sc07.commerce.order.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TraceService {

    private final Tracer tracer;

    public TraceService(Tracer tracer) {
        this.tracer = tracer;
    }

    public Optional<String> currentTraceId() {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return Optional.empty();
        }

        return Optional.of(currentSpan.context().traceId());
    }

    public Span startSpan(String name) {
        return tracer.nextSpan()
                .name(name)
                .start();
    }
}
