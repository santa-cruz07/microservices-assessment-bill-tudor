package com.sc07.commerce.order.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc07.commerce.shared.v1.OrderEvents;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(OrderEvents.EXCHANGE).durable(true).build();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "com.sc07.commerce.shared.v1");
    }
}
