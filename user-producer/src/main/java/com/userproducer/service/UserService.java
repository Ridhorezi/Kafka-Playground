package com.userproducer.service;

import com.userproducer.dto.UserCreateRequestDTO;
import com.userproducer.dto.UserResponseDTO;
import com.userproducer.dto.UserUpdateRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

public interface UserService {
	
	UserResponseDTO createUser(UserCreateRequestDTO request, String performedBy, HttpServletRequest servletRequest);

	UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request, String performedBy,
			HttpServletRequest servletRequest);

	void deleteUser(Long id, String performedBy, HttpServletRequest servletRequest);

	UserResponseDTO getUserById(Long id);

	Page<UserResponseDTO> getAllUsers(Integer offset, Integer limit);
}