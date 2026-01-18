package com.userproducer.controller;

import com.userproducer.dto.ApiResponseDTO;
import com.userproducer.dto.DepartmentRequestDTO;
import com.userproducer.dto.DepartmentResponseDTO;
import com.userproducer.dto.PaginatedResponseDTO;
import com.userproducer.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Department Management", description = "APIs for managing departments")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

	private final DepartmentService departmentService;

	@Operation(summary = "Create a new department", description = "Creates a new department with unique code")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Department created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "409", description = "Department code/name already exists") })
	@PostMapping
	public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> createDepartment(
			@Valid @RequestBody DepartmentRequestDTO request) {

		DepartmentResponseDTO response = departmentService.createDepartment(request);

		ApiResponseDTO<DepartmentResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Department created successfully");

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

	@Operation(summary = "Update department", description = "Updates department details")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Department updated successfully"),
			@ApiResponse(responseCode = "404", description = "Department not found"),
			@ApiResponse(responseCode = "409", description = "Department code/name already exists") })
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> updateDepartment(
			@Parameter(description = "Department ID") @PathVariable Long id,
			@Valid @RequestBody DepartmentRequestDTO request) {

		DepartmentResponseDTO response = departmentService.updateDepartment(id, request);

		ApiResponseDTO<DepartmentResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Department updated successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Delete department", description = "Deletes a department by ID")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Department deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Department not found") })
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<Void>> deleteDepartment(
			@Parameter(description = "Department ID") @PathVariable Long id) {

		departmentService.deleteDepartment(id);

		ApiResponseDTO<Void> apiResponse = ApiResponseDTO.success(null, "Department deleted successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get department by ID", description = "Retrieves department details by ID")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Department not found") })
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> getDepartmentById(
			@Parameter(description = "Department ID") @PathVariable Long id) {

		DepartmentResponseDTO response = departmentService.getDepartmentById(id);

		ApiResponseDTO<DepartmentResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Department retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get department by code", description = "Retrieves department details by department code")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Department not found") })
	@GetMapping("/code/{departmentCode}")
	public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> getDepartmentByCode(
			@Parameter(description = "Department code") @PathVariable String departmentCode) {

		DepartmentResponseDTO response = departmentService.getDepartmentByCode(departmentCode);

		ApiResponseDTO<DepartmentResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Department retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get all departments", description = "Retrieves all departments with pagination")
	@GetMapping
	public ResponseEntity<PaginatedResponseDTO<DepartmentResponseDTO>> getAllDepartments(
			@Parameter(description = "Offset for pagination", example = "0") @RequestParam(defaultValue = "0") Integer offset,
			@Parameter(description = "Limit per page (max 100)", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

		Page<DepartmentResponseDTO> departments = departmentService.getAllDepartments(offset, limit);

		PaginatedResponseDTO.PaginationInfo pagination = new PaginatedResponseDTO.PaginationInfo();

		pagination.setLimit(limit);
		pagination.setOffset(offset);
		pagination.setNextOffset(offset + limit < departments.getTotalElements() ? offset + limit : null);
		pagination.setTotalPage((int) Math.ceil((double) departments.getTotalElements() / limit));

		PaginatedResponseDTO<DepartmentResponseDTO> response = PaginatedResponseDTO.success(departments.getContent(),
				pagination, "Departments retrieved successfully");

		return ResponseEntity.ok(response);
	}
}