package com.userproducer.service;

import com.userproducer.dto.DepartmentRequestDTO;
import com.userproducer.dto.DepartmentResponseDTO;
import org.springframework.data.domain.Page;

public interface DepartmentService {

	DepartmentResponseDTO createDepartment(DepartmentRequestDTO request);

	DepartmentResponseDTO updateDepartment(Long id, DepartmentRequestDTO request);

	void deleteDepartment(Long id);

	DepartmentResponseDTO getDepartmentById(Long id);

	DepartmentResponseDTO getDepartmentByCode(String departmentCode);

	Page<DepartmentResponseDTO> getAllDepartments(Integer offset, Integer limit);
}