package com.userproducer.config;

import com.userproducer.constans.Constants;
import com.userproducer.dto.UserEventDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

	@Value("${spring.kafka.producer.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public NewTopic userCreatedTopic() {
		return new NewTopic(Constants.USER_CREATED_TOPIC, 3, (short) 1);
	}

	@Bean
	public NewTopic userUpdatedTopic() {
		return new NewTopic(Constants.USER_UPDATED_TOPIC, 3, (short) 1);
	}

	@Bean
	public NewTopic userDeletedTopic() {
		return new NewTopic(Constants.USER_DELETED_TOPIC, 3, (short) 1);
	}

	@Bean
	public ProducerFactory<String, UserEventDTO> userEventProducerFactory() {

		Map<String, Object> config = new HashMap<>();

		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		config.put(ProducerConfig.ACKS_CONFIG, "all");
		config.put(ProducerConfig.RETRIES_CONFIG, 3);
		config.put(ProducerConfig.LINGER_MS_CONFIG, 1);
		config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	public KafkaTemplate<String, UserEventDTO> userEventKafkaTemplate() {
		return new KafkaTemplate<>(userEventProducerFactory());
	}
}