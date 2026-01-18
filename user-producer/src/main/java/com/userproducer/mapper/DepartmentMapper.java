package com.userproducer.mapper;

import com.userproducer.dto.DepartmentRequestDTO;
import com.userproducer.dto.DepartmentResponseDTO;
import com.userproducer.model.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {
    
    public Department toEntity(DepartmentRequestDTO dto) {
        Department department = new Department();
        department.setDepartmentCode(dto.getDepartmentCode());
        department.setDepartmentName(dto.getDepartmentName());
        department.setDescription(dto.getDescription());
        department.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        return department;
    }
    
    public DepartmentResponseDTO toResponseDTO(Department department) {
        return new DepartmentResponseDTO(
            department.getId(),
            department.getDepartmentCode(),
            department.getDepartmentName(),
            department.getDescription(),
            department.getStatus(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );
    }
}