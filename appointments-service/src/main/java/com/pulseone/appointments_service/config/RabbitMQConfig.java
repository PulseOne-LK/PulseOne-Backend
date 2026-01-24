package com.pulseone.appointments_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.pulseone.appointments_service.events.UserEventListener;

/**
 * RabbitMQ configuration for the Appointments Service
 * Declares queues, exchanges, and bindings for event consumption
 */
@Configuration
public class RabbitMQConfig {

    public static final String USER_EVENTS_EXCHANGE = "user-events-exchange";
    public static final String USER_REGISTRATION_QUEUE = "user-registration-events-appointments";
    public static final String USER_REGISTRATION_ROUTING_KEY = "user.registration.#";

    public static final String CLINIC_UPDATE_EXCHANGE = "user-events-exchange";
    public static final String CLINIC_UPDATE_QUEUE = "clinic-update-events-appointments";
    public static final String CLINIC_UPDATE_ROUTING_KEY = "clinic.update.#";

    public static final String VIDEO_CONSULTATION_QUEUE = "video-consultation-events-appointments";
    public static final String VIDEO_CONSULTATION_ROUTING_KEY = "appointment.consultation.completed";

    /**
     * Declare the user events exchange
     */
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Declare the user registration queue for appointments service
     */
    @Bean
    public Queue userRegistrationQueue() {
        return new Queue(USER_REGISTRATION_QUEUE, true, false, false);
    }

    /**
     * Declare the clinic update queue for appointments service
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

    /**
     * Declare the video consultation queue for appointments service
     * Listens for video consultation completion events from video service
     */
    @Bean
    public Queue videoConsultationQueue() {
        return new Queue(VIDEO_CONSULTATION_QUEUE, true, false, false);
    }

    /**
     * Bind the video consultation queue to the exchange
     */
    @Bean
    public Binding videoConsultationBinding(Queue videoConsultationQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(videoConsultationQueue)
                .to(userEventsExchange)
                .with(VIDEO_CONSULTATION_ROUTING_KEY);
    }
}
