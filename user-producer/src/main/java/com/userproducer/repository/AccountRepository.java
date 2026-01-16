package com.userproducer.repository;

import com.userproducer.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
	
	Optional<Account> findByUserId(Long userId);

	boolean existsByUserId(Long userId);

	@Modifying
	@Transactional
	@Query("DELETE FROM Account a WHERE a.userId = :userId")
	int deleteByUserId(Long userId);
}