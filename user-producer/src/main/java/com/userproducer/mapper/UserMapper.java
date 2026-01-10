package com.userproducer.mapper;

import com.userproducer.dto.UserCreateRequestDTO;
import com.userproducer.dto.UserResponseDTO;
import com.userproducer.dto.UserEventDTO;
import com.userproducer.model.User;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class UserMapper {
    
    public User toEntity(UserCreateRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhoneNumber(dto.getPhoneNumber());
        return user;
    }
    
    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.getPhoneNumber(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    
    public UserEventDTO toEventDTO(User user, String eventType, String performedBy, String ipAddress) {
        return new UserEventDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            eventType,
            "User " + eventType.toLowerCase() + " successfully",
            performedBy,
            LocalDateTime.now(),
            ipAddress
        );
    }
}