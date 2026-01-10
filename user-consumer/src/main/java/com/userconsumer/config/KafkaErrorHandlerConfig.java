package com.userconsumer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

	@Bean
	public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {

		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
				(record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition()));

		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(3000L, 3));

		handler.addNotRetryableExceptions(DataIntegrityViolationException.class);

		handler.addNotRetryableExceptions(IllegalArgumentException.class);

		handler.setRetryListeners((record, ex, deliveryAttempt) -> {
			log.warn("Failed record: {}, Attempt: {}, Exception: {}", record.value(), deliveryAttempt, ex.getMessage());
		});

		return handler;
	}
}