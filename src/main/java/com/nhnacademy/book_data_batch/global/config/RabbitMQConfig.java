package com.nhnacademy.book_data_batch.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "discount.exchange";
    public static final String QUEUE = "discount.policy.reprice.queue";
    public static final String ROUTING_KEY = "discount.policy.changed";

    public static final String STORAGE_EXCHANGE = "storage.exchange";
    public static final String STORAGE_DESCRIPTION_QUEUE = "storage.image.uploaded.description.queue";
    public static final String STORAGE_DESCRIPTION_ROUTING_KEY = "storage.image.uploaded.description";
    public static final String STORAGE_DLX = "storage.dlx";
    public static final String STORAGE_DESCRIPTION_DLQ = "storage.description.dlq";
    public static final String STORAGE_DESCRIPTION_FAILED_ROUTING_KEY = "storage.description.failed";

    public static final String DISCOUNT_DLQ = "discount.policy.reprice.dlq";
    public static final String DISCOUNT_DLX = "discount.dlx";
    public static final String DISCOUNT_FAILED_ROUTING_KEY = "discount.failed";

    @Bean
    public TopicExchange discountExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue discountPolicyQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DISCOUNT_DLX)
                .withArgument("x-dead-letter-routing-key", DISCOUNT_FAILED_ROUTING_KEY)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding discountPolicyBinding() {
        return BindingBuilder
                .bind(discountPolicyQueue())
                .to(discountExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public TopicExchange storageExchange() {
        return new TopicExchange(STORAGE_EXCHANGE);
    }

    @Bean
    public Queue storageDescriptionQueue() {
        return QueueBuilder.durable(STORAGE_DESCRIPTION_QUEUE)
                .withArgument("x-dead-letter-exchange", STORAGE_DLX)
                .withArgument("x-dead-letter-routing-key", STORAGE_DESCRIPTION_FAILED_ROUTING_KEY)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding storageDescriptionBinding() {
        return BindingBuilder
                .bind(storageDescriptionQueue())
                .to(storageExchange())
                .with(STORAGE_DESCRIPTION_ROUTING_KEY);
    }

    @Bean
    public DirectExchange storageDlx() {
        return new DirectExchange(STORAGE_DLX);
    }

    @Bean
    public Queue storageDescriptionDlq() {
        return QueueBuilder.durable(STORAGE_DESCRIPTION_DLQ).build();
    }

    @Bean
    public Binding storageDlqBinding() {
        return BindingBuilder
                .bind(storageDescriptionDlq())
                .to(storageDlx())
                .with(STORAGE_DESCRIPTION_FAILED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange discountDlx() {
        return new DirectExchange(DISCOUNT_DLX);
    }

    @Bean
    public Queue discountDlq() {
        return QueueBuilder.durable(DISCOUNT_DLQ).build();
    }

    @Bean
    public Binding discountDlqBinding() {
        return BindingBuilder
                .bind(discountDlq())
                .to(discountDlx())
                .with(DISCOUNT_FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter converter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
