package com.userproducer.service;

import com.userproducer.dto.UserCreateRequestDTO;
import com.userproducer.dto.UserResponseDTO;
import com.userproducer.dto.UserUpdateRequestDTO;
import com.userproducer.exception.DuplicateResourceException;
import com.userproducer.exception.ResourceNotFoundException;
import com.userproducer.mapper.UserMapper;
import com.userproducer.model.User;
import com.userproducer.repository.UserRepository;
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
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final KafkaProducerService kafkaProducerService;

	@Transactional
	public UserResponseDTO createUser(UserCreateRequestDTO request, String performedBy,
			HttpServletRequest servletRequest) {

		// Check for duplicates
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Email already exists: " + request.getEmail());
		}

		if (userRepository.existsByUsername(request.getUsername())) {
			throw new DuplicateResourceException("Username already exists: " + request.getUsername());
		}

		// Create user
		User user = userMapper.toEntity(request);
		User savedUser = userRepository.save(user);

		log.info("User created successfully with ID: {}", savedUser.getId());

		// Get client IP
		String ipAddress = getClientIP(servletRequest);

		// Send event to Kafka
		try {
			kafkaProducerService.sendUserCreatedEvent(savedUser, performedBy, ipAddress);
			log.info("Kafka event sent for user: {}", savedUser.getId());
		} catch (Exception e) {
			log.error("Failed to send Kafka event for user: {}", savedUser.getId(), e);
		}

		return userMapper.toResponseDTO(savedUser);
	}

	@Transactional
	public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request, String performedBy,
			HttpServletRequest servletRequest) {

		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		// Check for duplicate email if email is being updated
		if (StringUtils.hasText(request.getEmail()) && !user.getEmail().equals(request.getEmail())
				&& userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Email already exists: " + request.getEmail());
		}

		// Check for duplicate username if username is being updated
		if (StringUtils.hasText(request.getUsername()) && !user.getUsername().equals(request.getUsername())
				&& userRepository.existsByUsername(request.getUsername())) {
			throw new DuplicateResourceException("Username already exists: " + request.getUsername());
		}

		// Update fields if provided
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

		User updatedUser = userRepository.save(user);

		log.info("User updated successfully with ID: {}", updatedUser.getId());

		// Get client IP
		String ipAddress = getClientIP(servletRequest);

		// Send event to Kafka
		try {
			kafkaProducerService.sendUserUpdatedEvent(updatedUser, performedBy, ipAddress);
			log.info("Kafka update event sent for user: {}", updatedUser.getId());
		} catch (Exception e) {
			log.error("Failed to send Kafka update event for user: {}", updatedUser.getId(), e);
		}

		return userMapper.toResponseDTO(updatedUser);
	}

	@Transactional
	public void deleteUser(Long id, String performedBy, HttpServletRequest servletRequest) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		// Get client IP before deleting
		String ipAddress = getClientIP(servletRequest);

		// Send event to Kafka before deletion
		try {
			kafkaProducerService.sendUserDeletedEvent(user, performedBy, ipAddress);
			log.info("Kafka delete event sent for user: {}", user.getId());
		} catch (Exception e) {
			log.error("Failed to send Kafka delete event for user: {}", user.getId(), e);
		}

		// Delete user
		userRepository.delete(user);

		log.info("User deleted successfully with ID: {}", id);
	}

	public UserResponseDTO getUserById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		return userMapper.toResponseDTO(user);
	}

	public Page<UserResponseDTO> getAllUsers(Integer offset, Integer limit) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		Page<User> users = userRepository.findAll(pageable);
		return users.map(userMapper::toResponseDTO);
	}

	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader != null && !xfHeader.isEmpty()) {
			return xfHeader.split(",")[0];
		}
		return request.getRemoteAddr();
	}
}