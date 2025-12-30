package com.pulseone.inventory_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Inventory Service
 * Declares queues, exchanges, and bindings for prescription dispensing events
 */
@Configuration
public class RabbitMQConfig {

    public static final String PRESCRIPTION_EVENTS_EXCHANGE = "prescription-events-exchange";
    public static final String PRESCRIPTION_DISPENSED_QUEUE = "prescription-dispensed-events";
    public static final String PRESCRIPTION_DISPENSED_ROUTING_KEY = "prescription.dispensed.#";

    /**
     * Declare the prescription events exchange
     */
    @Bean
    public TopicExchange prescriptionEventsExchange() {
        return new TopicExchange(PRESCRIPTION_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Declare the prescription dispensed queue
     */
    @Bean
    public Queue prescriptionDispensedQueue() {
        return new Queue(PRESCRIPTION_DISPENSED_QUEUE, true, false, false);
    }

    /**
     * Bind the prescription dispensed queue to the exchange
     */
    @Bean
    public Binding prescriptionDispensedBinding(Queue prescriptionDispensedQueue,
            TopicExchange prescriptionEventsExchange) {
        return BindingBuilder.bind(prescriptionDispensedQueue)
                .to(prescriptionEventsExchange)
                .with(PRESCRIPTION_DISPENSED_ROUTING_KEY);
    }
}
