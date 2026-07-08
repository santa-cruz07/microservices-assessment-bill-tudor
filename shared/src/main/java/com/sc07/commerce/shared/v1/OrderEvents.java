package com.sc07.commerce.shared.v1;

/** AMQP naming shared by producer and consumer. */
public final class OrderEvents {

    public static final String EXCHANGE = "orders.events";

    /** Tenant id travels as an AMQP header so the consumer can establish its
     * tenant context before touching its own data layer. */
    public static final String TENANT_HEADER = "x-tenant-id";

    public static String routingKey(OrderEventType type) {
        return switch (type) {
            case ORDER_CREATED -> "order.created";
            case ORDER_STATUS_CHANGED -> "order.status-changed";
            case ORDER_CANCELLED -> "order.cancelled";
        };
    }

    private OrderEvents() {
    }
}
