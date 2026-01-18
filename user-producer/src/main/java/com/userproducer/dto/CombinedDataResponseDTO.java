package com.userproducer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombinedDataResponseDTO {

	// User fields
	private Long userId;
	private String username;
	private String userEmail;
	private String phoneNumber;

	// Account fields
	private Long accountId;
	private String accountNumber;
	private String accountType;
	private Double balance;
	private String accountStatus;

	// Employee fields
	private Long employeeId;
	private String employeeCode;
	private String position;
	private Double salary;
	private String employeeStatus;

	// Department fields
	private Long departmentId;
	private String departmentCode;
	private String departmentName;
	private String departmentDescription;
	private String departmentStatus;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime employeeHireDate;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime accountCreatedAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime userCreatedAt;
}