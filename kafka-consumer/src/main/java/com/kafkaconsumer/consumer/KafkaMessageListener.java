package com.kafkaconsumer.consumer;

import com.kafkaconsumer.dto.Customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageListener {

	private static final Logger log = LoggerFactory.getLogger(KafkaMessageListener.class);

	@KafkaListener(topics = "spring-topic-customer", containerFactory = "customerKafkaListenerContainerFactory")
	public void consumeCustomer(Customer customer, Acknowledgment ack) {

		try {

			log.info("Consumed customer id={}, email={}", customer.getId(), customer.getEmail());

			ack.acknowledge();

		} catch (Exception e) {
			log.error("Failed processing customer {}", customer.getId(), e);
		}
	}

	@KafkaListener(topics = "spring-topic-string", containerFactory = "stringKafkaListenerContainerFactory")
	public void consumeString(String message, Acknowledgment ack) {

		try {

			log.info("Consumed string => {}", message);

			ack.acknowledge();

		} catch (Exception e) {
			log.error("Failed processing message {}", message);
		}
	}

	@KafkaListener(topics = "spring-topic-customer-dlt", containerFactory = "customerDltKafkaListenerContainerFactory")
	public void consume(Customer customer, Acknowledgment ack) {

		if (customer.getEmail() == null) {
			throw new IllegalArgumentException("Email is null");
		}

		if (!customer.getEmail().endsWith("@gmail.com")) {
			throw new RuntimeException("Invalid gmail email domain");
		}

		log.info("DLT DEMO SUCCESS {}", customer);
		
		ack.acknowledge();
	}
}
