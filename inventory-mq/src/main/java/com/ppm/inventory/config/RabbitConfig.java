package com.ppm.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${inventory.mq.exchange:inventory.exchange}")
    private String exchangeName;

    @Value("${inventory.mq.queue:inventory.supplier.queue}")
    private String queueName;

    @Value("${inventory.mq.routing-key:inventory.supplier}")
    private String routingKey;

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(inventoryQueue).to(inventoryExchange).with(routingKey);
    }
}
