package com.userproducer.service.impl;

import com.userproducer.dto.EmployeeRequestDTO;
import com.userproducer.dto.EmployeeResponseDTO;
import com.userproducer.dto.CombinedDataResponseDTO;
import com.userproducer.exception.ResourceNotFoundException;
import com.userproducer.exception.DuplicateResourceException;
import com.userproducer.mapper.EmployeeMapper;
import com.userproducer.model.*;
import com.userproducer.repository.*;
import com.userproducer.service.EmployeeService;
import com.userproducer.utility.Util;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final AccountRepository accountRepository;
	private final EmployeeMapper employeeMapper;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {

		try {
			// Check if user exists
			User user = userRepository.findById(request.getUserId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

			// Check if department exists
			Department department = departmentRepository.findById(request.getDepartmentId()).orElseThrow(
					() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

			// Check if employee already exists for this user
			Optional<Employee> existingEmployee = employeeRepository.findByUserId(request.getUserId());

			if (existingEmployee.isPresent()) {
				throw new DuplicateResourceException("Employee already exists for user id: " + request.getUserId());
			}

			// Create employee
			Employee employee = new Employee();
			employee.setEmployeeCode(generateEmployeeCode());
			employee.setUser(user);
			employee.setDepartment(department);
			employee.setPosition(request.getPosition());
			employee.setSalary(request.getSalary());
			employee.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

			Employee savedEmployee = employeeRepository.save(employee);

			// Get account if exists
			Optional<Account> account = accountRepository.findByUserId(request.getUserId());

			Util.logInfo("EmployeeService", "Employee created: {}", savedEmployee.getEmployeeCode());

			return employeeMapper.toResponseDTO(savedEmployee, user, department, account.orElse(null));

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to create employee: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO request) {

		try {

			Employee employee = employeeRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

			// Update department if changed
			if (!employee.getDepartment().getId().equals(request.getDepartmentId())) {
				Department department = departmentRepository.findById(request.getDepartmentId())
						.orElseThrow(() -> new ResourceNotFoundException(
								"Department not found with id: " + request.getDepartmentId()));
				employee.setDepartment(department);
			}

			// Update other fields
			employee.setPosition(request.getPosition());
			employee.setSalary(request.getSalary());
			employee.setStatus(request.getStatus() != null ? request.getStatus() : employee.getStatus());

			Employee updatedEmployee = employeeRepository.save(employee);

			// Get account if exists
			Optional<Account> account = accountRepository.findByUserId(employee.getUser().getId());

			Util.logInfo("EmployeeService", "Employee updated: {}", updatedEmployee.getEmployeeCode());

			return employeeMapper.toResponseDTO(updatedEmployee, employee.getUser(), updatedEmployee.getDepartment(),
					account.orElse(null));

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to update employee: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteEmployee(Long id) {

		try {

			Employee employee = employeeRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

			employeeRepository.delete(employee);

			Util.logInfo("EmployeeService", "Employee deleted: {}", employee.getEmployeeCode());

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to delete employee: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public EmployeeResponseDTO getEmployeeById(Long id) {

		try {

			Employee employee = employeeRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

			// Get account if exists
			Optional<Account> account = accountRepository.findByUserId(employee.getUser().getId());

			return employeeMapper.toResponseDTO(employee, employee.getUser(), employee.getDepartment(),
					account.orElse(null));

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to get employee by id: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public EmployeeResponseDTO getEmployeeByUserId(Long userId) {

		try {

			Employee employee = employeeRepository.findByUserId(userId)
					.orElseThrow(() -> new ResourceNotFoundException("Employee not found for user id: " + userId));

			// Get account if exists
			Optional<Account> account = accountRepository.findByUserId(userId);

			return employeeMapper.toResponseDTO(employee, employee.getUser(), employee.getDepartment(),
					account.orElse(null));

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to get employee by user id: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public Page<EmployeeResponseDTO> getAllEmployees(Integer offset, Integer limit) {

		try {

			int pageLimit = Math.min(limit, 100);

			Pageable pageable = PageRequest.of(offset / pageLimit, pageLimit);

			Page<Employee> employees = employeeRepository.findAll(pageable);

			return employees.map(employee -> {
				Optional<Account> account = accountRepository.findByUserId(employee.getUser().getId());
				return employeeMapper.toResponseDTO(employee, employee.getUser(), employee.getDepartment(),
						account.orElse(null));
			});

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to get all employees: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public List<CombinedDataResponseDTO> getAllCombinedData() {
		try {
			Query query = entityManager.createNativeQuery("""
					SELECT
					    u.id as userId,
					    u.username,
					    u.email as userEmail,
					    u.phone_number as phoneNumber,
					    u.created_at as userCreatedAt,
					    a.id as accountId,
					    a.account_number as accountNumber,
					    a.account_type as accountType,
					    a.balance,
					    a.status as accountStatus,
					    a.created_at as accountCreatedAt,
					    e.id as employeeId,
					    e.employee_code as employeeCode,
					    e.position,
					    e.salary,
					    e.status as employeeStatus,
					    e.hire_date as employeeHireDate,
					    d.id as departmentId,
					    d.department_code as departmentCode,
					    d.department_name as departmentName,
					    d.description as departmentDescription,
					    d.status as departmentStatus
					FROM employees e
					JOIN users u ON e.user_id = u.id
					JOIN departments d ON e.department_id = d.id
					LEFT JOIN accounts a ON u.id = a.user_id
					WHERE e.status = 'ACTIVE'
					ORDER BY e.created_at DESC
					""");

			List<Object[]> results = query.getResultList();

			return mapNativeQueryResults(results);

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to get combined data: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get combined data: " + e.getMessage(), e);
		}
	}

	@Override
	public List<CombinedDataResponseDTO> getCombinedDataByDepartment(Long departmentId) {
		try {
			Query query = entityManager.createNativeQuery("""
					SELECT
					    u.id as userId,
					    u.username,
					    u.email as userEmail,
					    u.phone_number as phoneNumber,
					    u.created_at as userCreatedAt,
					    a.id as accountId,
					    a.account_number as accountNumber,
					    a.account_type as accountType,
					    a.balance,
					    a.status as accountStatus,
					    a.created_at as accountCreatedAt,
					    e.id as employeeId,
					    e.employee_code as employeeCode,
					    e.position,
					    e.salary,
					    e.status as employeeStatus,
					    e.hire_date as employeeHireDate,
					    d.id as departmentId,
					    d.department_code as departmentCode,
					    d.department_name as departmentName,
					    d.description as departmentDescription,
					    d.status as departmentStatus
					FROM employees e
					JOIN users u ON e.user_id = u.id
					JOIN departments d ON e.department_id = d.id
					LEFT JOIN accounts a ON u.id = a.user_id
					WHERE d.id = :departmentId AND e.status = 'ACTIVE'
					ORDER BY e.created_at DESC
					""");

			query.setParameter("departmentId", departmentId);

			List<Object[]> results = query.getResultList();

			return mapNativeQueryResults(results);

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to get combined data by department: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get combined data by department: " + e.getMessage(), e);
		}
	}

	@Override
	public List<CombinedDataResponseDTO> getCombinedDataByUser(Long userId) {
		try {
			Query query = entityManager.createNativeQuery("""
					SELECT
					    u.id as userId,
					    u.username,
					    u.email as userEmail,
					    u.phone_number as phoneNumber,
					    u.created_at as userCreatedAt,
					    a.id as accountId,
					    a.account_number as accountNumber,
					    a.account_type as accountType,
					    a.balance,
					    a.status as accountStatus,
					    a.created_at as accountCreatedAt,
					    e.id as employeeId,
					    e.employee_code as employeeCode,
					    e.position,
					    e.salary,
					    e.status as employeeStatus,
					    e.hire_date as employeeHireDate,
					    d.id as departmentId,
					    d.department_code as departmentCode,
					    d.department_name as departmentName,
					    d.description as departmentDescription,
					    d.status as departmentStatus
					FROM employees e
					JOIN users u ON e.user_id = u.id
					JOIN departments d ON e.department_id = d.id
					LEFT JOIN accounts a ON u.id = a.user_id
					WHERE u.id = :userId AND e.status = 'ACTIVE'
					""");

			query.setParameter("userId", userId);

			List<Object[]> results = query.getResultList();

			return mapNativeQueryResults(results);

		} catch (Exception e) {
			Util.logError("EmployeeService", "Failed to get combined data by user: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get combined data by user: " + e.getMessage(), e);
		}
	}

	private List<CombinedDataResponseDTO> mapNativeQueryResults(List<Object[]> results) {
		return results.stream().map(this::createResultMap).map(employeeMapper::toCombinedDataResponseDTO)
				.collect(Collectors.toList());
	}

	private Map<String, Object> createResultMap(Object[] result) {
		Map<String, Object> resultMap = new LinkedHashMap<>();

		// Mapping berdasarkan urutan SELECT di query
		// Pastikan urutan ini sama dengan query SQL di atas
		resultMap.put("userId", result[0]);
		resultMap.put("username", result[1]);
		resultMap.put("userEmail", result[2]);
		resultMap.put("phoneNumber", result[3]);
		resultMap.put("userCreatedAt", result[4]);
		resultMap.put("accountId", result[5]);
		resultMap.put("accountNumber", result[6]);
		resultMap.put("accountType", result[7]);
		resultMap.put("balance", result[8]);
		resultMap.put("accountStatus", result[9]);
		resultMap.put("accountCreatedAt", result[10]);
		resultMap.put("employeeId", result[11]);
		resultMap.put("employeeCode", result[12]);
		resultMap.put("position", result[13]);
		resultMap.put("salary", result[14]);
		resultMap.put("employeeStatus", result[15]);
		resultMap.put("employeeHireDate", result[16]);
		resultMap.put("departmentId", result[17]);
		resultMap.put("departmentCode", result[18]);
		resultMap.put("departmentName", result[19]);
		resultMap.put("departmentDescription", result[20]);
		resultMap.put("departmentStatus", result[21]);

		return resultMap;
	}

	private String generateEmployeeCode() {
		return "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}