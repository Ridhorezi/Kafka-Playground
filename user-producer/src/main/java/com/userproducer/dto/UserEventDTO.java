package com.userproducer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEventDTO {
	
    private Long userId;
    private String username;
    private String email;
    private String eventType;
    private String description;
    private String performedBy;
    private LocalDateTime eventTimestamp;
    private String ipAddress;
}