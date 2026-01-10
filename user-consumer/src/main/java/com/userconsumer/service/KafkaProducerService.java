package com.userconsumer.service;

import com.userconsumer.dto.UserEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

	private final AuditService auditService;

	@KafkaListener(topics = "user-created-topic", containerFactory = "userEventKafkaListenerContainerFactory")
	public void consumeUserCreatedEvent(@Payload UserEventDTO event, Acknowledgment acknowledgment) {

		try {

			log.info("Received user created event for user: {}", event.getUserId());

			// Save audit trail
			auditService.saveAuditTrail(event);

			// Manually acknowledge
			acknowledgment.acknowledge();

			log.info("Successfully processed user created event for user: {}", event.getUserId());

		} catch (Exception e) {
			log.error("Error processing user created event for user: {}", event.getUserId(), e);
		}
	}

	// DLT Listener
	@KafkaListener(topics = "user-created-topic.dlt", containerFactory = "userEventKafkaListenerContainerFactory")
	public void consumeUserCreatedEventDLT(@Payload UserEventDTO event, Acknowledgment acknowledgment) {

		log.error("DLT - Failed to process user event after retries: UserId={}, Error={}", event.getUserId(),
				event.getDescription());

		acknowledgment.acknowledge();
	}
}
