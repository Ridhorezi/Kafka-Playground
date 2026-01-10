package com.userconsumer.service;

import com.userconsumer.dto.UserEventDTO;
import com.userconsumer.model.AuditTrail;
import com.userconsumer.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditTrailRepository auditTrailRepository;

	@Transactional
	public void saveAuditTrail(UserEventDTO event) {

		try {

			AuditTrail auditTrail = new AuditTrail();

			auditTrail.setUserId(event.getUserId());
			auditTrail.setUsername(event.getUsername());
			auditTrail.setEmail(event.getEmail());
			auditTrail.setEventType(event.getEventType());
			auditTrail.setDescription(event.getDescription());
			auditTrail.setPerformedBy(event.getPerformedBy());
			auditTrail.setIpAddress(event.getIpAddress());
			auditTrail.setCreatedAt(event.getEventTimestamp());

			auditTrailRepository.save(auditTrail);

			log.info("Audit trail saved successfully for user: {}", event.getUserId());

		} catch (Exception e) {

			log.error("Failed to save audit trail for user: {}", event.getUserId(), e);

			throw e;
		}
	}
}
