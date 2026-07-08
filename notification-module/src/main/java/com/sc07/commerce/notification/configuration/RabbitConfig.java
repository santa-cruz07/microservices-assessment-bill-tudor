package com.sc07.commerce.notification.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.shared.v1.OrderEvents;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "notifications.order-events";
    public static final String DLX = "orders.events.dlx";
    public static final String DLQ = "notifications.order-events.dlq";

    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(OrderEvents.EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderEventsQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).build();
    }

    @Bean
    public Binding orderEventsBinding(Queue orderEventsQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderEventsQueue).to(ordersExchange).with("order.#");
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return ExchangeBuilder.fanoutExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "com.loadup");
    }
}
