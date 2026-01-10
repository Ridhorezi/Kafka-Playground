package com.kafkaproducer.controller;

import com.kafkaproducer.dto.Customer;
import com.kafkaproducer.service.KafkaMessagePublisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/producer-app")
public class EventController {

	@Autowired
	private KafkaMessagePublisher publisher;

	@GetMapping("/publish/{message}")
	public ResponseEntity<?> publishMessage(@PathVariable String message) {

		try {

			publisher.sendMessage(message);

			return ResponseEntity.ok("message published successfully ..");

		} catch (Exception ex) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/publish")
	public ResponseEntity<String> sendEvents(@RequestBody Customer customer) {
		try {

			publisher.sendCustomer(customer);

			return ResponseEntity.ok("message published successfully ..");

		} catch (Exception ex) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/publish/dlt-demo")
	public ResponseEntity<String> publishCustomerDltDemo(@RequestBody Customer customer) {

		publisher.sendCustomerDltDemo(customer);
		
		return ResponseEntity.ok("DLT demo event sent");
	}

}