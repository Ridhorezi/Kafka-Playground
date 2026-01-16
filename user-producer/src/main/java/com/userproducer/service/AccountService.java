package com.userproducer.service;

import com.userproducer.model.User;

public interface AccountService {
    
    /**
     * Create account for user
     * @param user User object
     */
    void createAccount(User user);
    
    /**
     * Rollback account creation
     * @param userId User ID
     */
    void rollbackAccount(Long userId);
}