package com.userproducer.service.impl;

import com.userproducer.dto.UserCreateRequestDTO;
import com.userproducer.dto.UserResponseDTO;
import com.userproducer.dto.UserUpdateRequestDTO;
import com.userproducer.exception.DuplicateResourceException;
import com.userproducer.exception.ResourceNotFoundException;
import com.userproducer.mapper.UserMapper;
import com.userproducer.model.User;
import com.userproducer.repository.UserRepository;
import com.userproducer.service.AccountService;
import com.userproducer.service.KafkaProducerService;
import com.userproducer.service.UserService;
import com.userproducer.utility.Util;
import com.userproducer.workflow.WorkflowBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final KafkaProducerService kafkaProducerService;
	private final AccountService accountService;

	@Override
	@Transactional
	public UserResponseDTO createUser(UserCreateRequestDTO request, String performedBy,
			HttpServletRequest servletRequest) {

		String ipAddress = Util.getClientIP(servletRequest);

		String workflowId = "USER-" + System.currentTimeMillis();

		try {
			return executeUserCreationWorkflow(request, performedBy, ipAddress, workflowId);
		} catch (Exception e) {
			throw e;
		}
	}

	private UserResponseDTO executeUserCreationWorkflow(UserCreateRequestDTO request, String performedBy,
			String ipAddress, String workflowId) {

		// Default Validasi
		validateUserRules(request);

		// Define base WorkflowBuilder
		WorkflowBuilder workflow = new WorkflowBuilder().name("CreateUserWithAccount").log(true)
				.with("workflow_id", workflowId).with("request", request).with("performedBy", performedBy)
				.with("ipAddress", ipAddress);

		return workflow

				// Step 1: Create user
				.step("Create User Entity", () -> createUser(request), this::rollbackUser)

				// Step 2: Create account
				.step("Create User Account", () -> {
					User user = workflow.getLastResult();
					accountService.createAccount(user);
					return user;
				}, this::rollbackAccount)

				// Step 3: Send audit event
				.step("Send Audit Event", () -> {
					User user = workflow.getLastResult();
					sendAuditEvent(user, performedBy, ipAddress);
					return user;
				})

				// Step 4: Build response
				.step("Build Response", () -> buildUserResponse(workflow.getLastResult()))

				// Finish
				.execute();
	}

	private void validateUserRules(UserCreateRequestDTO request) {

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Email already exists: " + request.getEmail());
		}

		if (userRepository.existsByUsername(request.getUsername())) {
			throw new DuplicateResourceException("Username already exists: " + request.getUsername());
		}
	}

	private User createUser(UserCreateRequestDTO request) {

		User user = userMapper.toEntity(request);

		User savedUser = userRepository.save(user);

		Util.logInfo("UserService", "User created with ID: {}", savedUser.getId());

		return savedUser;
	}

	private void rollbackUser(User savedUser) {

		Util.logInfo("UserService", "Rolling back user creation: {}", savedUser.getId());

		userRepository.delete(savedUser);
	}

	private void rollbackAccount(User user) {

		Util.logInfo("UserService", "Rolling back account creation for user: {}", user.getId());

		accountService.rollbackAccount(user.getId());
	}

	private User sendAuditEvent(User user, String performedBy, String ipAddress) {

		try {
			kafkaProducerService.sendUserCreatedEvent(user, performedBy, ipAddress);
			Util.logInfo("UserService", "Audit event sent for user: {}", user.getId());
		} catch (Exception e) {
			Util.logWarn("UserService", "Failed to send audit event: {}", e.getMessage());
		}

		return user;
	}

	private UserResponseDTO buildUserResponse(User user) {

		return userMapper.toResponseDTO(user);
	}

	@Override
	@Transactional
	public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request, String performedBy,
			HttpServletRequest servletRequest) {

		User user = findUserById(id);

		validateUserUpdate(user, request);

		updateUserFields(user, request);

		User updatedUser = userRepository.save(user);

		Util.logInfo("UserService", "User updated with ID: {}", updatedUser.getId());

		sendUpdateEvent(updatedUser, performedBy, Util.getClientIP(servletRequest));

		return userMapper.toResponseDTO(updatedUser);
	}

	@Override
	@Transactional
	public void deleteUser(Long id, String performedBy, HttpServletRequest servletRequest) {

		User user = findUserById(id);

		String ipAddress = Util.getClientIP(servletRequest);

		sendDeleteEvent(user, performedBy, ipAddress);

		deleteAssociatedAccount(id);

		userRepository.delete(user);

		Util.logInfo("UserService", "User deleted with ID: {}", id);
	}

	@Override
	public UserResponseDTO getUserById(Long id) {

		User user = findUserById(id);

		return userMapper.toResponseDTO(user);
	}

	@Override
	public Page<UserResponseDTO> getAllUsers(Integer offset, Integer limit) {

		validatePaginationParams(offset, limit);

		Pageable pageable = PageRequest.of(offset / limit, limit);

		Page<User> users = userRepository.findAll(pageable);

		Util.logInfo("UserService", "Retrieved {} users", users.getTotalElements());

		return users.map(userMapper::toResponseDTO);
	}

	// PRIVATE HELPER METHODS

	private User findUserById(Long id) {

		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
	}

	private void validateUserUpdate(User user, UserUpdateRequestDTO request) {

		if (StringUtils.hasText(request.getEmail()) && !user.getEmail().equals(request.getEmail())
				&& userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Email already exists");
		}

		if (StringUtils.hasText(request.getUsername()) && !user.getUsername().equals(request.getUsername())
				&& userRepository.existsByUsername(request.getUsername())) {
			throw new DuplicateResourceException("Username already exists");
		}
	}

	private void updateUserFields(User user, UserUpdateRequestDTO request) {

		if (StringUtils.hasText(request.getUsername())) {
			user.setUsername(request.getUsername());
		}

		if (StringUtils.hasText(request.getEmail())) {
			user.setEmail(request.getEmail());
		}

		if (StringUtils.hasText(request.getPassword())) {
			user.setPassword(request.getPassword());
		}

		if (StringUtils.hasText(request.getPhoneNumber())) {
			user.setPhoneNumber(request.getPhoneNumber());
		}

		user.setUpdatedAt(LocalDateTime.now());
	}

	private void sendUpdateEvent(User updatedUser, String performedBy, String ipAddress) {

		try {
			kafkaProducerService.sendUserUpdatedEvent(updatedUser, performedBy, ipAddress);
			Util.logInfo("UserService", "Update event sent for user: {}", updatedUser.getId());
		} catch (Exception e) {
			Util.logError("UserService", "Failed to send update event: {}", e.getMessage());
		}
	}

	private void sendDeleteEvent(User user, String performedBy, String ipAddress) {
		try {
			kafkaProducerService.sendUserDeletedEvent(user, performedBy, ipAddress);
			Util.logInfo("UserService", "Delete event sent for user: {}", user.getId());
		} catch (Exception e) {
			Util.logError("UserService", "Failed to send delete event: {}", e.getMessage());
		}
	}

	private void deleteAssociatedAccount(Long userId) {
		try {
			accountService.rollbackAccount(userId);
		} catch (Exception e) {
			Util.logWarn("UserService", "Account not found for user: {}", userId);
		}
	}

	private void validatePaginationParams(Integer offset, Integer limit) {

		if (offset == null || offset < 0) {
			throw new IllegalArgumentException("Offset must be >= 0");
		}

		if (limit == null || limit <= 0 || limit > 100) {
			throw new IllegalArgumentException("Limit must be between 1 and 100");
		}
	}
}

