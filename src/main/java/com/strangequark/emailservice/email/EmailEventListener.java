package com.strangequark.emailservice.email;

import com.strangequark.emailservice.utility.TelemetryUtility; // Integration line: Telemetry
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header; // Integration line: Auth
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Integration line: Telemetry
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.mock.web.MockHttpServletRequest; // Integration line: Auth
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.context.request.RequestContextHolder; // Integration line: Auth
import org.springframework.web.context.request.ServletRequestAttributes; // Integration line: Auth

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class EmailEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailEventListener.class);

    private final EmailService emailService;
    // Integration function start: Telemetry
    @Autowired
    TelemetryUtility telemetryUtility;
    // Integration function end: Telemetry

    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // Initialize the email-events topics on startup
    @Bean
    public Collection<NewTopic> kafkaTopics() {
        return List.of(
                TopicBuilder.name("general-email-events").partitions(1).replicas(1).build(),
                TopicBuilder.name("token-email-events").partitions(1).replicas(1).build(),
                TopicBuilder.name("register-email-events").partitions(1).replicas(1).build(),
                TopicBuilder.name("password-reset-email-events").partitions(1).replicas(1).build()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailRequest> kafkaListenerContainerFactory(ConsumerFactory<String, EmailRequest> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EmailRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));

        return factory;
    }


    @KafkaListener(topics = "general-email-events", groupId = "email-group")
    public void generalEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("General email event received");
        EmailRequest emailRequest = record.value();
        setAuthHeaderFromKafkaConsumerRecord(record); // Integration line: Auth
        telemetryUtility.sendTelemetryEvent("email-event-general", // Integration function start: Telemetry
                true, // Integration line: Auth
                Map.of()
        ); // Integration function end: Telemetry

        emailService.send(emailRequest.getRecipient(), emailRequest.getSender(), emailRequest.getEmail(), emailRequest.getSubject());
    }

    @KafkaListener(topics = "token-email-events", groupId = "email-group")
    public void tokenEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("Token email event received");
        EmailRequest emailRequest = record.value();
        setAuthHeaderFromKafkaConsumerRecord(record); // Integration line: Auth
        telemetryUtility.sendTelemetryEvent("email-event-token", // Integration function start: Telemetry
                true, // Integration line: Auth
                Map.of()
        ); // Integration function end: Telemetry

        emailService.sendEmailWithToken(emailRequest, false, false);
    }

    @KafkaListener(topics = "register-email-events", groupId = "email-group")
    public void registerEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("Register email event received");
        EmailRequest emailRequest = record.value();
        setAuthHeaderFromKafkaConsumerRecord(record); // Integration line: Auth
        telemetryUtility.sendTelemetryEvent("email-event-register", // Integration function start: Telemetry
                true, // Integration line: Auth
                Map.of()
        ); // Integration function end: Telemetry

        emailService.sendEmailWithToken(emailRequest, true, false);
    }

    @KafkaListener(topics = "password-reset-email-events", groupId = "email-group")
    public void passwordResetEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("Password reset email event received");
        EmailRequest emailRequest = record.value();
        setAuthHeaderFromKafkaConsumerRecord(record); // Integration line: Auth
        telemetryUtility.sendTelemetryEvent("email-event-password-reset", // Integration function start: Telemetry
                true, // Integration line: Auth
                Map.of()
        ); // Integration function end: Telemetry

        emailService.sendEmailWithToken(emailRequest, false, true);
    }
    // Integration function start: Auth
    public void setAuthHeaderFromKafkaConsumerRecord(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("Setting authorization header from Kafka consumer record");

        // Extract JWT from Kafka header
        Header authHeader = record.headers().lastHeader("Authorization");
        if (authHeader == null) {
            LOGGER.error("Missing Authorization header in Kafka message");
            return;
        }
        String token = new String(authHeader.value());

        // Create a mock request with Authorization header
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", token);

        // Bind the mock request to the current thread
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);
        LOGGER.info("Kafka consumer auth header set");
    } // Integration function end: Auth
}
