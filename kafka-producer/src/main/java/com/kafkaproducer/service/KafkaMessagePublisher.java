package com.kafkaproducer.service;

import com.kafkaproducer.dto.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaMessagePublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaMessagePublisher.class);

	private final KafkaTemplate<String, Customer> customerKafkaTemplate;

	private final KafkaTemplate<String, String> stringKafkaTemplate;

	public KafkaMessagePublisher(KafkaTemplate<String, Customer> customerKafkaTemplate,
			KafkaTemplate<String, String> stringKafkaTemplate) {
		this.customerKafkaTemplate = customerKafkaTemplate;
		this.stringKafkaTemplate = stringKafkaTemplate;
	}

	public void sendCustomer(Customer customer) {

		CompletableFuture<SendResult<String, Customer>> future = customerKafkaTemplate.send("spring-topic-customer",
				customer);

		future.whenComplete((result, ex) -> {
			if (ex == null) {
				log.info("Customer sent successfully => key={}, partition={}, offset={}",
						result.getProducerRecord().key(), result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			} else {
				log.error("Failed to send customer => {}", customer, ex);
			}
		});
	}

	public void sendMessage(String message) {

		CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send("spring-topic-string", message);

		future.whenComplete((result, ex) -> {
			if (ex == null) {
				log.info("Message sent successfully => [{}], partition={}, offset={}", message,
						result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
			} else {
				log.error("Failed to send message => [{}]", message, ex);
			}
		});
	}
	
	public void sendCustomerDltDemo(Customer customer) {

	    CompletableFuture<SendResult<String, Customer>> future =
	            customerKafkaTemplate.send(
	                    "spring-topic-customer-dlt",
	                    customer
	            );

	    future.whenComplete((result, ex) -> {
	        if (ex == null) {
	            log.info("DLT-DEMO sent => partition={}, offset={}",
	                    result.getRecordMetadata().partition(),
	                    result.getRecordMetadata().offset());
	        } else {
	            log.error("DLT-DEMO failed sending {}", customer, ex);
	        }
	    });
	}

}
