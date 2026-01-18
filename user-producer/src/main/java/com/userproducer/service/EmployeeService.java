package com.userproducer.service;

import com.userproducer.dto.EmployeeRequestDTO;
import com.userproducer.dto.EmployeeResponseDTO;
import com.userproducer.dto.CombinedDataResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EmployeeService {

	EmployeeResponseDTO createEmployee(EmployeeRequestDTO request);

	EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO request);

	void deleteEmployee(Long id);

	EmployeeResponseDTO getEmployeeById(Long id);

	EmployeeResponseDTO getEmployeeByUserId(Long userId);

	Page<EmployeeResponseDTO> getAllEmployees(Integer offset, Integer limit);

	// Service untuk kombinasi data dari 4 tabel
	List<CombinedDataResponseDTO> getAllCombinedData();

	List<CombinedDataResponseDTO> getCombinedDataByDepartment(Long departmentId);

	List<CombinedDataResponseDTO> getCombinedDataByUser(Long userId);
}