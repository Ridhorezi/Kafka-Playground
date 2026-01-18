package com.userproducer.repository;

import com.userproducer.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

	// Standard JPA query methods
	Optional<Department> findByDepartmentCode(String departmentCode);

	Optional<Department> findByDepartmentName(String departmentName);

	boolean existsByDepartmentCode(String departmentCode);

	boolean existsByDepartmentName(String departmentName);

	// Native query 
	@Query(value = "SELECT * FROM departments WHERE status = 'ACTIVE'", nativeQuery = true)
	List<Department> findAllActiveDepartmentsNative();

	// JPQL query 
	@Query("SELECT d FROM Department d WHERE d.status = :status")
	List<Department> findByStatus(@Param("status") String status);
}