package com.userproducer.service.impl;

import com.userproducer.dto.DepartmentRequestDTO;
import com.userproducer.dto.DepartmentResponseDTO;
import com.userproducer.exception.DuplicateResourceException;
import com.userproducer.exception.ResourceNotFoundException;
import com.userproducer.mapper.DepartmentMapper;
import com.userproducer.model.Department;
import com.userproducer.repository.DepartmentRepository;
import com.userproducer.service.DepartmentService;
import com.userproducer.utility.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

	private final DepartmentRepository departmentRepository;
	private final DepartmentMapper departmentMapper;

	@Override
	@Transactional
	public DepartmentResponseDTO createDepartment(DepartmentRequestDTO request) {

		try {
			// Check if department code already exists
			if (departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
				throw new DuplicateResourceException(
						"Department with code " + request.getDepartmentCode() + " already exists");
			}

			// Check if department name already exists
			if (departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
				throw new DuplicateResourceException(
						"Department with name " + request.getDepartmentName() + " already exists");
			}

			// Create department
			Department department = departmentMapper.toEntity(request);

			Department savedDepartment = departmentRepository.save(department);

			Util.logInfo("DepartmentService", "Department created: {}", savedDepartment.getDepartmentCode());

			return departmentMapper.toResponseDTO(savedDepartment);

		} catch (Exception e) {
			Util.logError("DepartmentService", "Failed to create department: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public DepartmentResponseDTO updateDepartment(Long id, DepartmentRequestDTO request) {

		try {

			Department department = departmentRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

			// Check if new department code conflicts with existing
			if (!department.getDepartmentCode().equals(request.getDepartmentCode())
					&& departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
				throw new DuplicateResourceException(
						"Department with code " + request.getDepartmentCode() + " already exists");
			}

			// Check if new department name conflicts with existing
			if (!department.getDepartmentName().equals(request.getDepartmentName())
					&& departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
				throw new DuplicateResourceException(
						"Department with name " + request.getDepartmentName() + " already exists");
			}

			// Update department
			department.setDepartmentCode(request.getDepartmentCode());
			department.setDepartmentName(request.getDepartmentName());
			department.setDescription(request.getDescription());
			department.setStatus(request.getStatus() != null ? request.getStatus() : department.getStatus());

			Department updatedDepartment = departmentRepository.save(department);

			Util.logInfo("DepartmentService", "Department updated: {}", updatedDepartment.getDepartmentCode());

			return departmentMapper.toResponseDTO(updatedDepartment);

		} catch (Exception e) {
			Util.logError("DepartmentService", "Failed to update department: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteDepartment(Long id) {

		try {

			Department department = departmentRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

			departmentRepository.delete(department);

			Util.logInfo("DepartmentService", "Department deleted: {}", department.getDepartmentCode());

		} catch (Exception e) {
			Util.logError("DepartmentService", "Failed to delete department: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public DepartmentResponseDTO getDepartmentById(Long id) {

		try {

			Department department = departmentRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

			return departmentMapper.toResponseDTO(department);

		} catch (Exception e) {
			Util.logError("DepartmentService", "Failed to get department by id: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public DepartmentResponseDTO getDepartmentByCode(String departmentCode) {

		try {

			Department department = departmentRepository.findByDepartmentCode(departmentCode).orElseThrow(
					() -> new ResourceNotFoundException("Department not found with code: " + departmentCode));

			return departmentMapper.toResponseDTO(department);

		} catch (Exception e) {
			Util.logError("DepartmentService", "Failed to get department by code: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public Page<DepartmentResponseDTO> getAllDepartments(Integer offset, Integer limit) {

		try {
			// Validate limit
			int pageLimit = Math.min(limit, 100);

			Pageable pageable = PageRequest.of(offset / pageLimit, pageLimit);
			Page<Department> departments = departmentRepository.findAll(pageable);

			return departments.map(departmentMapper::toResponseDTO);

		} catch (Exception e) {
			Util.logError("DepartmentService", "Failed to get all departments: {}", e.getMessage(), e);
			throw e;
		}
	}
}