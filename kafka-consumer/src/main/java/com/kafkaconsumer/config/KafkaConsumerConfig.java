package com.kafkaconsumer.config;

import com.kafkaconsumer.dto.Customer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

	@Value("${spring.kafka.consumer.bootstrap-servers}")
	private String bootstrapServers;

	/* Object */

	@Bean
	public ConsumerFactory<String, Customer> customerConsumerFactory() {

		JsonDeserializer<Customer> valueDeserializer = new JsonDeserializer<>(Customer.class);

		valueDeserializer.addTrustedPackages("*");
		valueDeserializer.setUseTypeHeaders(false);

		Map<String, Object> props = new HashMap<>();

		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "customer-group");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
	}

	@Bean(name = "customerKafkaListenerContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, Customer> customerKafkaListenerContainerFactory() {

		ConcurrentKafkaListenerContainerFactory<String, Customer> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(customerConsumerFactory());
		factory.setConcurrency(3);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

		return factory;
	}

	/* String */

	@Bean
	public ConsumerFactory<String, String> stringConsumerFactory() {

		Map<String, Object> props = new HashMap<>();

		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "string-group");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean(name = "stringKafkaListenerContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {

		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(stringConsumerFactory());
		factory.setConcurrency(3);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

		return factory;
	}

	/* Dlt */

	@Bean
	public ConsumerFactory<String, Customer> customerDltConsumerFactory() {

		JsonDeserializer<Customer> valueDeserializer = new JsonDeserializer<>(Customer.class);

		valueDeserializer.addTrustedPackages("*");
		valueDeserializer.setUseTypeHeaders(false);

		Map<String, Object> props = new HashMap<>();

		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "customer-dlt-group");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
	}

	@Bean(name = "customerDltKafkaListenerContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, Customer> customerDltKafkaListenerContainerFactory(
			KafkaErrorHandlerConfig dltErrorHandler) {

		ConcurrentKafkaListenerContainerFactory<String, Customer> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(customerDltConsumerFactory());
		factory.setConcurrency(3);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

		return factory;
	}

}	
