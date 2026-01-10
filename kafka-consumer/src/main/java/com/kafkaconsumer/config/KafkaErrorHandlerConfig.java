package com.kafkaconsumer.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

	@Bean
	public DefaultErrorHandler dltErrorHandler(KafkaTemplate<Object, Object> template) {

		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
				(record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition()));

		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(3000L, 3));

		handler.addNotRetryableExceptions(IllegalArgumentException.class);

		return handler;
	}
}
