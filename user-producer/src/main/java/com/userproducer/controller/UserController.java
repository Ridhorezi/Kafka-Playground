package com.userproducer.controller;

import com.userproducer.dto.ApiResponseDTO;
import com.userproducer.dto.PaginatedResponseDTO;
import com.userproducer.dto.UserCreateRequestDTO;
import com.userproducer.dto.UserResponseDTO;
import com.userproducer.dto.UserUpdateRequestDTO;
import com.userproducer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping
	public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createUser(@Valid @RequestBody UserCreateRequestDTO request,
			@RequestHeader(value = "X-User-Id", defaultValue = "system") String performedBy,
			HttpServletRequest servletRequest) {

		UserResponseDTO response = userService.createUser(request, performedBy, servletRequest);

		ApiResponseDTO<UserResponseDTO> apiResponse = ApiResponseDTO.success(response, "User created successfully");

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(@PathVariable Long id,
			@Valid @RequestBody UserUpdateRequestDTO request,
			@RequestHeader(value = "X-User-Id", defaultValue = "system") String performedBy,
			HttpServletRequest servletRequest) {

		UserResponseDTO updatedUser = userService.updateUser(id, request, performedBy, servletRequest);

		ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.success(updatedUser, "User updated successfully");

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<Void>> deleteUser(@PathVariable Long id,
			@RequestHeader(value = "X-User-Id", defaultValue = "system") String performedBy,
			HttpServletRequest servletRequest) {

		userService.deleteUser(id, performedBy, servletRequest);

		ApiResponseDTO<Void> response = ApiResponseDTO.success(null, "User deleted successfully");

		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(@PathVariable Long id) {
		
		UserResponseDTO user = userService.getUserById(id);
		
		ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.success(user, "User retrieved successfully");
		
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getAllUsers(
			@RequestParam(defaultValue = "0") Integer offset, @RequestParam(defaultValue = "10") Integer limit) {

		Page<UserResponseDTO> users = userService.getAllUsers(offset, limit);

		PaginatedResponseDTO.PaginationInfo pagination = new PaginatedResponseDTO.PaginationInfo();
		
		pagination.setLimit(limit);
		pagination.setOffset(offset);
		pagination.setNextOffset(offset + limit < users.getTotalElements() ? offset + limit : null);
		pagination.setTotalPage((int) Math.ceil((double) users.getTotalElements() / limit));

		PaginatedResponseDTO<UserResponseDTO> response = PaginatedResponseDTO.success(users.getContent(), pagination,
				"Users retrieved successfully");

		return ResponseEntity.ok(response);
	}
}