package com.userproducer.service.impl;

import com.userproducer.model.Account;
import com.userproducer.model.User;
import com.userproducer.repository.AccountRepository;
import com.userproducer.service.AccountService;
import com.userproducer.utility.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

	private final AccountRepository accountRepository;

	@Override
	@Transactional
	public void createAccount(User user) {
		try {
			Account account = buildAccount(user);
			accountRepository.save(account);
			Util.logInfo("AccountService", "Account created for user: {}", user.getId());
		} catch (Exception e) {
			Util.logError("AccountService", "Failed to create account for user: {}", user.getId(), e);
			throw new RuntimeException("Failed to create account: " + e.getMessage());
		}
	}

	@Override
	@Transactional
	public void rollbackAccount(Long userId) {
		try {
			int deletedCount = accountRepository.deleteByUserId(userId);
			if (deletedCount > 0) {
				Util.logInfo("AccountService", "Account rollback successful for user: {}", userId);
			} else {
				Util.logWarn("AccountService", "No account found for user: {}", userId);
			}
		} catch (Exception e) {
			Util.logError("AccountService", "Failed to rollback account for user: {}", userId, e);
		}
	}

	private Account buildAccount(User user) {
		Account account = new Account();
		account.setUserId(user.getId());
		account.setAccountNumber(Util.generateAccountNumber());
		account.setUsername(user.getUsername());
		account.setEmail(user.getEmail());
		account.setStatus("ACTIVE");
		account.setBalance(10000.0);
		account.setCreatedAt(LocalDateTime.now());
		return account;
	}
}