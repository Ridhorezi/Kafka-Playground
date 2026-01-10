package com.userconsumer.config;

import com.userconsumer.dto.UserEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

	@Value("${spring.kafka.consumer.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public ConsumerFactory<String, UserEventDTO> userEventConsumerFactory() {

		JsonDeserializer<UserEventDTO> valueDeserializer = new JsonDeserializer<>(UserEventDTO.class);
		
		valueDeserializer.addTrustedPackages("com.userproducer.dto");
		valueDeserializer.setUseTypeHeaders(false);

		Map<String, Object> props = new HashMap<>();
		
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-group");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, UserEventDTO> userEventKafkaListenerContainerFactory() {

		ConcurrentKafkaListenerContainerFactory<String, UserEventDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(userEventConsumerFactory());
		factory.setConcurrency(3);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
		factory.setBatchListener(false);

		return factory;
	}
}
