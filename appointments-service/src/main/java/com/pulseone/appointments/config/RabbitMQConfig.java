package com.pulseone.appointments.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for consuming Protobuf events
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name
    public static final String USER_EVENTS_EXCHANGE = "user-events-exchange";

    // Queue names
    public static final String USER_REGISTRATION_QUEUE = "user-registration-events";
    public static final String CLINIC_UPDATE_QUEUE = "clinic-update-events";

    // Routing keys
    public static final String USER_REGISTRATION_ROUTING_KEY = "user.registration.#";
    public static final String CLINIC_UPDATE_ROUTING_KEY = "clinic.update.#";

    /**
     * Declare the topic exchange for user events
     */
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Declare the user registration queue (durable)
     */
    @Bean
    public Queue userRegistrationQueue() {
        return new Queue(USER_REGISTRATION_QUEUE, true, false, false);
    }

    /**
     * Declare the clinic update queue (durable)
     */
    @Bean
    public Queue clinicUpdateQueue() {
        return new Queue(CLINIC_UPDATE_QUEUE, true, false, false);
    }

    /**
     * Bind the user registration queue to the exchange
     */
    @Bean
    public Binding userRegistrationBinding(Queue userRegistrationQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userRegistrationQueue)
                .to(userEventsExchange)
                .with(USER_REGISTRATION_ROUTING_KEY);
    }

    /**
     * Bind the clinic update queue to the exchange
     */
    @Bean
    public Binding clinicUpdateBinding(Queue clinicUpdateQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(clinicUpdateQueue)
                .to(userEventsExchange)
                .with(CLINIC_UPDATE_ROUTING_KEY);
    }
}
