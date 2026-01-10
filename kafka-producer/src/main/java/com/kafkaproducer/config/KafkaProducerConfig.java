package com.kafkaproducer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import com.kafkaproducer.dto.Customer;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic customerTopic() {
        return new NewTopic("spring-topic-customer", 3, (short) 1);
    }
    
    @Bean
    public NewTopic customerDltTopic() {
        return new NewTopic("spring-topic-customer.dlt", 3, (short) 1);
    }

    @Bean
    public NewTopic stringTopic() {
        return new NewTopic("spring-topic-string", 3, (short) 1);
    }

    @Bean
    public ProducerFactory<String, Customer> customerProducerFactory() {

        Map<String, Object> props = new HashMap<>();
        
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public ProducerFactory<String, Customer> customerDltProducerFactory() {

        Map<String, Object> props = new HashMap<>();
        
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {

        Map<String, Object> props = new HashMap<>();
        
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Customer> customerKafkaTemplate() {
        return new KafkaTemplate<>(customerProducerFactory());
    }
    
    @Bean
    public KafkaTemplate<String, Customer> customerDltKafkaTemplate() {
        return new KafkaTemplate<>(customerProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }
}
