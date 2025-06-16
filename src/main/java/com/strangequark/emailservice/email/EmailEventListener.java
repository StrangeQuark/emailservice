package com.strangequark.emailservice.email;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Collection;
import java.util.List;

@Service
public class EmailEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailEventListener.class);

    private final EmailService emailService;

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
    public void generalEmailEvents(EmailRequest emailRequest) {
        LOGGER.info("General email event received");

        emailService.send(emailRequest.getRecipient(), emailRequest.getSender(), emailRequest.getEmail(), emailRequest.getSubject());
    }

    @KafkaListener(topics = "token-email-events", groupId = "email-group")
    public void tokenEmailEvents(EmailRequest emailRequest) {
        LOGGER.info("Token email event received");

        emailService.sendEmailWithToken(emailRequest, false, false);
    }

    @KafkaListener(topics = "register-email-events", groupId = "email-group")
    public void registerEmailEvents(EmailRequest emailRequest) {
        LOGGER.info("Register email event received");

        emailService.sendEmailWithToken(emailRequest, true, false);
    }

    @KafkaListener(topics = "password-reset-email-events", groupId = "email-group")
    public void passwordResetEmailEvents(EmailRequest emailRequest) {
        LOGGER.info("Password reset email event received");

        emailService.sendEmailWithToken(emailRequest, false, true);
    }
}
