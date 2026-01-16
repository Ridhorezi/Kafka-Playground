package com.userproducer.controller;

import com.userproducer.constans.Constants;
import com.userproducer.dto.ApiResponseDTO;
import com.userproducer.dto.PaginatedResponseDTO;
import com.userproducer.dto.UserCreateRequestDTO;
import com.userproducer.dto.UserResponseDTO;
import com.userproducer.dto.UserUpdateRequestDTO;
import com.userproducer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Management", description = "APIs for managing users with Kafka event publishing")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "Create a new user", description = "Creates a new user account and publishes a USER_CREATED event to Kafka")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
			@ApiResponse(responseCode = "409", description = "User with email/username already exists", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))) })
	@PostMapping
	public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createUser(@Valid @RequestBody UserCreateRequestDTO request,
			@Parameter(description = "ID of user performing the action", example = "admin-123") @RequestHeader(value = "X-User-Id", defaultValue = "system") String performedBy,
			HttpServletRequest servletRequest) {

		try {
			
			UserResponseDTO response = userService.createUser(request, performedBy, servletRequest);
			
			ApiResponseDTO<UserResponseDTO> apiResponse = ApiResponseDTO.success(response,
					Constants.USER_CREATED_SUCCESSFULLY);
			
			return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
			
		} catch (Exception ex) {
			
			throw ex;
		}
	}

	@Operation(summary = "Update an existing user", description = "Updates user details and publishes a USER_UPDATED event to Kafka")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
			@ApiResponse(responseCode = "409", description = "Email/username already exists", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))) })
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
			@Parameter(description = "User ID", example = "1") @PathVariable Long id,
			@Valid @RequestBody UserUpdateRequestDTO request,
			@Parameter(description = "ID of user performing the action", example = "admin-123") @RequestHeader(value = "X-User-Id", defaultValue = "system") String performedBy,
			HttpServletRequest servletRequest) {

		UserResponseDTO updatedUser = userService.updateUser(id, request, performedBy, servletRequest);
		
		ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.success(updatedUser,
				Constants.USER_UPDATED_SUCCESSFULLY);
		
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete a user", description = "Deletes a user and publishes a USER_DELETED event to Kafka")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User deleted successfully", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))) })
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<Void>> deleteUser(
			@Parameter(description = "User ID", example = "1") @PathVariable Long id,
			@Parameter(description = "ID of user performing the action", example = "admin-123") @RequestHeader(value = "X-User-Id", defaultValue = "system") String performedBy,
			HttpServletRequest servletRequest) {

		userService.deleteUser(id, performedBy, servletRequest);
		
		ApiResponseDTO<Void> response = ApiResponseDTO.success(null, Constants.USER_DELETED_SUCCESSFULLY);
		
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get user by ID", description = "Retrieves user details by ID. Results are cached in Redis for performance.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))) })
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(
			@Parameter(description = "User ID", example = "1") @PathVariable Long id) {

		UserResponseDTO user = userService.getUserById(id);
		
		ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.success(user, Constants.USER_RETRIEVED_SUCCESSFULLY);
		
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get all users with pagination", description = "Retrieves a paginated list of users. Results are cached in Redis for performance.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = PaginatedResponseDTO.class))) })
	@GetMapping
	public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getAllUsers(
			@Parameter(description = "Offset for pagination", example = "0") @RequestParam(defaultValue = "0") Integer offset,
			@Parameter(description = "Limit per page (max 100)", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

		Page<UserResponseDTO> users = userService.getAllUsers(offset, limit);

		PaginatedResponseDTO.PaginationInfo pagination = new PaginatedResponseDTO.PaginationInfo();
		
		pagination.setLimit(limit);
		pagination.setOffset(offset);
		pagination.setNextOffset(offset + limit < users.getTotalElements() ? offset + limit : null);
		pagination.setTotalPage((int) Math.ceil((double) users.getTotalElements() / limit));

		PaginatedResponseDTO<UserResponseDTO> response = PaginatedResponseDTO.success(users.getContent(), pagination,
				Constants.USER_RETRIEVED_SUCCESSFULLY);

		return ResponseEntity.ok(response);
	}
}