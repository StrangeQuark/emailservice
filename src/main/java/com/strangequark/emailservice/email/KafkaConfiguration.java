package com.strangequark.emailservice.email;

import com.strangequark.emailservice.template.EmailTemplateRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfiguration {
    @Bean
    public Collection<NewTopic> kafkaTopics() {
        return List.of(
                TopicBuilder.name("general-email-events").partitions(1).replicas(1).build(),
                TopicBuilder.name("template-email-events").partitions(1).replicas(1).build()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailRequest> generalFactory(
            ConsumerFactory<Object, Object> consumerFactory) {

        Map<String, Object> props = consumerFactory.getConfigurationProperties();

        JsonDeserializer<EmailRequest> deserializer = new JsonDeserializer<>(EmailRequest.class, false);
        deserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, EmailRequest> cf = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );

        ConcurrentKafkaListenerContainerFactory<String, EmailRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf); // This will work now
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailTemplateRequest> templateFactory(
            ConsumerFactory<Object, Object> consumerFactory) {

        Map<String, Object> props = consumerFactory.getConfigurationProperties();

        JsonDeserializer<EmailTemplateRequest> deserializer = new JsonDeserializer<>(EmailTemplateRequest.class, false);
        deserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, EmailTemplateRequest> cf = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );

        ConcurrentKafkaListenerContainerFactory<String, EmailTemplateRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));

        return factory;
    }
}
