package com.strangequark.emailservice.kafka;

import com.strangequark.emailservice.email.EmailRequest;
import com.strangequark.emailservice.email.EmailService;
import com.strangequark.emailservice.utility.JwtUtility; // Integration line: Auth
import com.strangequark.emailservice.utility.TelemetryUtility; // Integration line: Telemetry
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header; // Integration line: Auth
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

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

    @KafkaListener(topics = "general-email-events", groupId = "email-group")
    public void generalEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("General email event received");
        EmailRequest emailRequest = record.value();
        String userId = null;
        // Integration function start: Auth
        String token = getTokenFromKafkaConsumerRecord(record);
        if(!jwtUtility.validateEmailApiAccessToken(token)) {
            LOGGER.error("Invalid JWT token - general email event skipped");
            return;
        }
        userId = jwtUtility.extractId(token);
        // Integration function end: Auth
        // Integration function start: Telemetry
        telemetryUtility.sendTelemetryEvent("email-event-general", Map.of(
                    "userId", userId // Integration line: Auth
                )
        ); // Integration function end: Telemetry

        emailService.sendEmail(emailRequest, false, userId);
    }

    @KafkaListener(topics = "template-email-events", groupId = "email-group")
    public void templateEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("Template email event received");
        EmailRequest emailRequest = record.value();
        String userId = null;
        // Integration function start: Auth
        String token = getTokenFromKafkaConsumerRecord(record);
        if(!jwtUtility.validateEmailApiAccessToken(token)) {
            LOGGER.error("Invalid JWT token - template email event skipped");
            return;
        }
        userId = jwtUtility.extractId(token);
        // Integration function end: Auth
        // Integration function start: Telemetry
        telemetryUtility.sendTelemetryEvent("email-event-template", Map.of(
                        "userId", userId // Integration line: Auth
                )
        ); // Integration function end: Telemetry

        emailService.sendTemplateEmail(emailRequest, false, userId);
    }
    // Integration function start: Auth
    public String getTokenFromKafkaConsumerRecord(ConsumerRecord<String, ?> record) {
        LOGGER.info("Getting authorization token from Kafka consumer record");

        Header authHeader = record.headers().lastHeader("Authorization");
        if (authHeader == null) {
            LOGGER.error("Missing Authorization header in Kafka message");
            return null;
        }
        String token = new String(authHeader.value());

        if(!token.startsWith("Bearer ")) {
            LOGGER.error("Invalid Authorization header in Kafka message");
            return null;
        }

        LOGGER.info("Kafka consumer authorization token retrieved");
        return token.substring(7);
    } // Integration function end: Auth
}
