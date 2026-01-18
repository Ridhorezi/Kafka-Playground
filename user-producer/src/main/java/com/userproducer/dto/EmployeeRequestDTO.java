package com.userproducer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequestDTO {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Department ID is required")
    private Long departmentId;
    
    @NotBlank(message = "Position is required")
    private String position;
    
    private Double salary;
    
    private String status = "ACTIVE";
}