package com.userproducer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

	private Long id;
	private String username;
	private String email;
	private String password;
	private String phoneNumber;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;

	public UserResponseDTO(String username, String email, String password, String phoneNumber, LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.phoneNumber = phoneNumber;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
}