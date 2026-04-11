package org.example.msreportingcertification.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ===============================
    // ✅ 1. ObjectMapper (for LocalDateTime support)
    // ===============================
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // ===============================
    // ✅ 2. Message Converter
    // ===============================
    @Bean
    public MessageConverter messageConverter() {

        JacksonJsonMessageConverter converter =
                new JacksonJsonMessageConverter();

        DefaultClassMapper classMapper = new DefaultClassMapper();

        // ✅ Allow all packages
        classMapper.setTrustedPackages("*");

        // 🔥 KEY FIX: map producer DTO → consumer DTO
        classMapper.setIdClassMapping(Map.of(
                "org.example.msevaluation.dto.EvaluationResultDTO",
                org.example.msreportingcertification.dto.EvaluationResultDTO.class
        ));

        // ✅ fallback (optional but safe)
        classMapper.setDefaultType(
                org.example.msreportingcertification.dto.EvaluationResultDTO.class
        );

        converter.setClassMapper(classMapper);

        return converter;
    }

    // ===============================
    // ✅ 3. Listener Container Factory
    // ===============================
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        return factory;
    }
}