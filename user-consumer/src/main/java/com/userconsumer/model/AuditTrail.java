package com.userconsumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_trails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "username")
	private String username;

	@Column(name = "email")
	private String email;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "performed_by")
	private String performedBy;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}