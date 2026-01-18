package com.userproducer.repository;

import com.userproducer.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	// Standard JPA query methods
	Optional<Employee> findByEmployeeCode(String employeeCode);

	Optional<Employee> findByUserId(Long userId);

	List<Employee> findByDepartmentId(Long departmentId);

	List<Employee> findByStatus(String status);

	// Native query
	@Query(value = "SELECT * FROM employees e WHERE e.user_id = :userId", nativeQuery = true)
	Optional<Employee> findEmployeeByUserIdNative(@Param("userId") Long userId);

	// JPQL query
	@Query("SELECT e FROM Employee e WHERE e.user.id = :userId")
	Optional<Employee> findEmployeeByUserId(@Param("userId") Long userId);

	// Native query untuk kombinasi
	@Query(value = """
			SELECT e.id as employeeId, e.employee_code as employeeCode,
			       e.position, e.salary, e.status as employeeStatus,
			       u.id as userId, u.username, u.email as userEmail,
			       d.id as departmentId, d.department_code as departmentCode,
			       d.department_name as departmentName,
			       a.id as accountId, a.account_number as accountNumber,
			       a.balance, a.status as accountStatus
			FROM employees e
			JOIN users u ON e.user_id = u.id
			JOIN departments d ON e.department_id = d.id
			LEFT JOIN accounts a ON u.id = a.user_id
			WHERE e.status = 'ACTIVE'
			""", nativeQuery = true)
	List<Object[]> findAllCombinedDataNative();
}