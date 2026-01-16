package com.userproducer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(name = "account_number", nullable = false, unique = true)
	private String accountNumber;

	@Column(name = "account_type", nullable = false)
	private String accountType;

	@Column(name = "username")
	private String username;

	@Column(name = "email")
	private String email;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "balance", nullable = false)
	private Double balance;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (balance == null) {
			balance = 0.0;
		}
		if (status == null) {
			status = "ACTIVE";
		}
		if (accountType == null) {
			accountType = "USER";
		}
	}
}
