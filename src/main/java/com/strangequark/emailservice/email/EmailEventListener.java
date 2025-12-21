package com.strangequark.emailservice.email;

import com.strangequark.emailservice.template.EmailTemplateRequest;
import com.strangequark.emailservice.utility.JwtUtility; // Integration line: Auth
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
import java.util.Map; // Integration line: Telemetry

@Service
public class EmailEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailEventListener.class);

    private final EmailService emailService;
    // Integration function start: Telemetry
    @Autowired
    TelemetryUtility telemetryUtility;
    // Integration function end: Telemetry
    // Integration function start: Auth
    @Autowired
    JwtUtility jwtUtility;
    // Integration function end: Auth

    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // Initialize the email-events topics on startup
//    @Bean
//    public Collection<NewTopic> kafkaTopics() {
//        return List.of(
//                TopicBuilder.name("general-email-events").partitions(1).replicas(1).build(),
//                TopicBuilder.name("template-email-events").partitions(1).replicas(1).build()
//        );
//    }

//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, EmailRequest> generalFactory(ConsumerFactory<Object, Object> consumerFactory) {
//        ConcurrentKafkaListenerContainerFactory<String, EmailRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));
//        return factory;
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, EmailTemplateRequest> templateFactory(ConsumerFactory<Object, Object> consumerFactory) {
//        ConcurrentKafkaListenerContainerFactory<String, EmailTemplateRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));
//        return factory;
//    }

    @KafkaListener(topics = "general-email-events", groupId = "email-group", containerFactory = "generalFactory")
    public void generalEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("General email event received");
        EmailRequest emailRequest = record.value();
        setAuthHeaderFromKafkaConsumerRecord(record); // Integration line: Auth
        // Integration function start: Telemetry
        telemetryUtility.sendTelemetryEvent("email-event-general", Map.of(
                    "userId", jwtUtility.extractId() // Integration line: Auth
                )
        ); // Integration function end: Telemetry

        emailService.sendEmail(emailRequest, true);
    }

    @KafkaListener(topics = "template-email-events", groupId = "email-group", containerFactory = "templateFactory")
    public void templateEmailEvents(ConsumerRecord<String, EmailTemplateRequest> record) {
        LOGGER.info("Template email event received");
        EmailTemplateRequest emailRequest = record.value();
        setAuthHeaderFromKafkaConsumerRecord(record); // Integration line: Auth
        // Integration function start: Telemetry
        telemetryUtility.sendTelemetryEvent("email-event-template", Map.of(
                        "userId", jwtUtility.extractId() // Integration line: Auth
                )
        ); // Integration function end: Telemetry

        emailService.sendTemplateEmail(emailRequest, true);
    }
    // Integration function start: Auth
    public void setAuthHeaderFromKafkaConsumerRecord(ConsumerRecord<String, ?> record) {
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
