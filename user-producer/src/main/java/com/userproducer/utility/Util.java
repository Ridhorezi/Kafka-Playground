package com.userproducer.utility;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@UtilityClass
public class Util {

    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ACCOUNT_PREFIX = "ACC-";
    
    /**
     * Format LocalDateTime to string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_FORMATTER);
    }
    
    /**
     * Get client IP address from HttpServletRequest
     */
    public static String getClientIP(HttpServletRequest request) {
    	
        if (request == null) return "unknown";
        
        String xfHeader = request.getHeader("X-Forwarded-For");
        
        if (xfHeader != null && !xfHeader.trim().isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Generate unique account number
     */
    public static String generateAccountNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return ACCOUNT_PREFIX + timestamp + "-" + random;
    }
    
    /**
     * Generate trace ID for request tracking
     */
    public static String generateTraceId() {
        return "TRACE-" + 
               System.currentTimeMillis() + "-" + 
               ThreadLocalRandom.current().nextInt(1000, 9999);
    }
    
    /**
     * Validate email format (basic validation)
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    /**
     * Mask sensitive data for logging
     */
    public static String maskSensitive(String data) {
        if (data == null || data.length() <= 4) return "****";
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
    
    /**
     * Log info with context
     */
    public static void logInfo(String component, String message, Object... args) {
        log.info("[{}] {}", component, String.format(message, args));
    }
    
    /**
     * Log error with context
     */
    public static void logError(String component, String message, Object... args) {
        log.error("[{}] {}", component, String.format(message, args));
    }
    
    /**
     * Log warning with context
     */
    public static void logWarn(String component, String message, Object... args) {
        log.warn("[{}] {}", component, String.format(message, args));
    }
}