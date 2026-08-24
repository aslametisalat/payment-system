package com.payment.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Sends TransactionCompletedEvent as a plain JSON text message rather than
 * a serialized Java object - the same wire format every other hop in this
 * project uses (REST/JSON), and readable by any consumer regardless of
 * classpath, not just one running the exact same class bytecode.
 */
@Configuration
public class JmsConfig {

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setObjectMapper(objectMapper);
        // Embeds the Java class as a JMS message property so consumers on the
        // same class (all of them, via the shared common module) know what
        // to deserialize into - without this, fromMessage() has no way to
        // know the target type and throws MessageConversionException.
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
