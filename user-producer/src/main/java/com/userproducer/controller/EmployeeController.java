package com.userproducer.controller;

import com.userproducer.dto.ApiResponseDTO;
import com.userproducer.dto.CombinedDataResponseDTO;
import com.userproducer.dto.EmployeeRequestDTO;
import com.userproducer.dto.EmployeeResponseDTO;
import com.userproducer.dto.PaginatedResponseDTO;
import com.userproducer.service.EmployeeService;
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

import java.util.List;

@Tag(name = "Employee Management", description = "APIs for managing employees")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

	private final EmployeeService employeeService;

	@Operation(summary = "Create a new employee", description = "Creates a new employee record linked to user and department")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Employee created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "404", description = "User or Department not found"),
			@ApiResponse(responseCode = "409", description = "Employee already exists for this user") })
	@PostMapping
	public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> createEmployee(
			@Valid @RequestBody EmployeeRequestDTO request) {

		EmployeeResponseDTO response = employeeService.createEmployee(request);

		ApiResponseDTO<EmployeeResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Employee created successfully");

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

	@Operation(summary = "Update employee", description = "Updates employee details")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
			@ApiResponse(responseCode = "404", description = "Employee not found") })
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> updateEmployee(
			@Parameter(description = "Employee ID") @PathVariable Long id,
			@Valid @RequestBody EmployeeRequestDTO request) {

		EmployeeResponseDTO response = employeeService.updateEmployee(id, request);

		ApiResponseDTO<EmployeeResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Employee updated successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Delete employee", description = "Deletes an employee by ID")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Employee deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Employee not found") })
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<Void>> deleteEmployee(
			@Parameter(description = "Employee ID") @PathVariable Long id) {

		employeeService.deleteEmployee(id);

		ApiResponseDTO<Void> apiResponse = ApiResponseDTO.success(null, "Employee deleted successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get employee by ID", description = "Retrieves employee details by ID")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Employee retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Employee not found") })
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> getEmployeeById(
			@Parameter(description = "Employee ID") @PathVariable Long id) {

		EmployeeResponseDTO response = employeeService.getEmployeeById(id);

		ApiResponseDTO<EmployeeResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Employee retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get employee by user ID", description = "Retrieves employee details by user ID")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Employee retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Employee not found for this user") })
	@GetMapping("/user/{userId}")
	public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> getEmployeeByUserId(
			@Parameter(description = "User ID") @PathVariable Long userId) {

		EmployeeResponseDTO response = employeeService.getEmployeeByUserId(userId);

		ApiResponseDTO<EmployeeResponseDTO> apiResponse = ApiResponseDTO.success(response,
				"Employee retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get all employees", description = "Retrieves all employees with pagination")
	@GetMapping
	public ResponseEntity<PaginatedResponseDTO<EmployeeResponseDTO>> getAllEmployees(
			@Parameter(description = "Offset for pagination", example = "0") @RequestParam(defaultValue = "0") Integer offset,
			@Parameter(description = "Limit per page (max 100)", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

		Page<EmployeeResponseDTO> employees = employeeService.getAllEmployees(offset, limit);

		PaginatedResponseDTO.PaginationInfo pagination = new PaginatedResponseDTO.PaginationInfo();

		pagination.setLimit(limit);
		pagination.setOffset(offset);
		pagination.setNextOffset(offset + limit < employees.getTotalElements() ? offset + limit : null);
		pagination.setTotalPage((int) Math.ceil((double) employees.getTotalElements() / limit));

		PaginatedResponseDTO<EmployeeResponseDTO> response = PaginatedResponseDTO.success(employees.getContent(),
				pagination, "Employees retrieved successfully");

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get combined data", description = "Retrieves combined data from all 4 tables (User, Account, Employee, Department)")
	@GetMapping("/combined-data")
	public ResponseEntity<ApiResponseDTO<List<CombinedDataResponseDTO>>> getCombinedData() {

		List<CombinedDataResponseDTO> response = employeeService.getAllCombinedData();

		ApiResponseDTO<List<CombinedDataResponseDTO>> apiResponse = ApiResponseDTO.success(response,
				"Combined data retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get combined data by department", description = "Retrieves combined data filtered by department")
	@GetMapping("/combined-data/department/{departmentId}")
	public ResponseEntity<ApiResponseDTO<List<CombinedDataResponseDTO>>> getCombinedDataByDepartment(
			@Parameter(description = "Department ID") @PathVariable Long departmentId) {

		List<CombinedDataResponseDTO> response = employeeService.getCombinedDataByDepartment(departmentId);

		ApiResponseDTO<List<CombinedDataResponseDTO>> apiResponse = ApiResponseDTO.success(response,
				"Combined data by department retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get combined data by user", description = "Retrieves combined data filtered by user")
	@GetMapping("/combined-data/user/{userId}")
	public ResponseEntity<ApiResponseDTO<List<CombinedDataResponseDTO>>> getCombinedDataByUser(
			@Parameter(description = "User ID") @PathVariable Long userId) {

		List<CombinedDataResponseDTO> response = employeeService.getCombinedDataByUser(userId);

		ApiResponseDTO<List<CombinedDataResponseDTO>> apiResponse = ApiResponseDTO.success(response,
				"Combined data by user retrieved successfully");

		return ResponseEntity.ok(apiResponse);
	}
}