package com.userproducer.service;

import com.userproducer.constans.Constants;
import com.userproducer.dto.UserEventDTO;
import com.userproducer.mapper.UserMapper;
import com.userproducer.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

	private final KafkaTemplate<String, UserEventDTO> kafkaTemplate;
	private final UserMapper userMapper;

	public void sendUserCreatedEvent(User user, String performedBy, String ipAddress) {
		sendUserEvent(user, "CREATED", performedBy, ipAddress, Constants.USER_CREATED_TOPIC);
	}

	public void sendUserUpdatedEvent(User user, String performedBy, String ipAddress) {
		sendUserEvent(user, "UPDATED", performedBy, ipAddress, Constants.USER_UPDATED_TOPIC);
	}

	public void sendUserDeletedEvent(User user, String performedBy, String ipAddress) {
		sendUserEvent(user, "DELETED", performedBy, ipAddress, Constants.USER_DELETED_TOPIC);
	}

	private void sendUserEvent(User user, String eventType, String performedBy, String ipAddress, String topic) {

		UserEventDTO event = userMapper.toEventDTO(user, eventType, performedBy, ipAddress);

		CompletableFuture<SendResult<String, UserEventDTO>> future = kafkaTemplate.send(topic, user.getId().toString(),
				event);

		future.whenComplete((result, ex) -> {
			if (ex == null) {
				log.info("User {} event sent successfully - UserId: {}, Partition: {}, Offset: {}",
						eventType.toLowerCase(), user.getId(), result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			} else {
				log.error("Failed to send user {} event - UserId: {}", eventType.toLowerCase(), user.getId(), ex);
			}
		});
	}
}