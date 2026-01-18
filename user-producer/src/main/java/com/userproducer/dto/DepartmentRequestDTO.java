package com.userproducer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequestDTO {

	@NotBlank(message = "Department code is required")
	@Size(min = 2, max = 10, message = "Department code must be between 2 and 10 characters")
	private String departmentCode;

	@NotBlank(message = "Department name is required")
	@Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
	private String departmentName;

	private String description;

	private String status = "ACTIVE";
}