package com.userproducer.mapper;

import com.userproducer.dto.EmployeeResponseDTO;
import com.userproducer.dto.CombinedDataResponseDTO;
import com.userproducer.model.Employee;
import com.userproducer.model.User;
import com.userproducer.model.Department;
import com.userproducer.model.Account;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class EmployeeMapper {

	public EmployeeResponseDTO toResponseDTO(Employee employee, User user, Department department, Account account) {
		EmployeeResponseDTO dto = new EmployeeResponseDTO();
		dto.setId(employee.getId());
		dto.setEmployeeCode(employee.getEmployeeCode());
		dto.setUserId(user != null ? user.getId() : null);
		dto.setUsername(user != null ? user.getUsername() : null);
		dto.setUserEmail(user != null ? user.getEmail() : null);
		dto.setDepartmentId(department != null ? department.getId() : null);
		dto.setDepartmentCode(department != null ? department.getDepartmentCode() : null);
		dto.setDepartmentName(department != null ? department.getDepartmentName() : null);
		dto.setPosition(employee.getPosition());
		dto.setSalary(employee.getSalary());
		dto.setStatus(employee.getStatus());
		dto.setHireDate(employee.getHireDate());
		dto.setCreatedAt(employee.getCreatedAt());
		dto.setUpdatedAt(employee.getUpdatedAt());
		return dto;
	}

	public CombinedDataResponseDTO toCombinedDataResponseDTO(Map<String, Object> resultMap) {
		CombinedDataResponseDTO dto = new CombinedDataResponseDTO();

		// User data
		dto.setUserId(getLongValue(resultMap.get("userId")));
		dto.setUsername(getStringValue(resultMap.get("username")));
		dto.setUserEmail(getStringValue(resultMap.get("userEmail")));
		dto.setPhoneNumber(getStringValue(resultMap.get("phoneNumber")));

		// Employee data
		dto.setEmployeeId(getLongValue(resultMap.get("employeeId")));
		dto.setEmployeeCode(getStringValue(resultMap.get("employeeCode")));
		dto.setPosition(getStringValue(resultMap.get("position")));
		dto.setSalary(getDoubleValue(resultMap.get("salary")));
		dto.setEmployeeStatus(getStringValue(resultMap.get("employeeStatus")));

		// Department data
		dto.setDepartmentId(getLongValue(resultMap.get("departmentId")));
		dto.setDepartmentCode(getStringValue(resultMap.get("departmentCode")));
		dto.setDepartmentName(getStringValue(resultMap.get("departmentName")));
		dto.setDepartmentDescription(getStringValue(resultMap.get("departmentDescription")));
		dto.setDepartmentStatus(getStringValue(resultMap.get("departmentStatus")));

		// Account data
		dto.setAccountId(getLongValue(resultMap.get("accountId")));
		dto.setAccountNumber(getStringValue(resultMap.get("accountNumber")));
		dto.setAccountType(getStringValue(resultMap.get("accountType")));
		dto.setBalance(getDoubleValue(resultMap.get("balance")));
		dto.setAccountStatus(getStringValue(resultMap.get("accountStatus")));

		// Timestamps - handle different types
		dto.setEmployeeHireDate(convertToLocalDateTime(resultMap.get("employeeHireDate")));
		dto.setAccountCreatedAt(convertToLocalDateTime(resultMap.get("accountCreatedAt")));
		dto.setUserCreatedAt(convertToLocalDateTime(resultMap.get("userCreatedAt")));

		return dto;
	}

	private Long getLongValue(Object value) {
		if (value == null)
			return null;
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double getDoubleValue(Object value) {
		if (value == null)
			return null;
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String getStringValue(Object value) {
		if (value == null)
			return null;
		return value.toString();
	}

	private LocalDateTime convertToLocalDateTime(Object value) {
		if (value == null)
			return null;

		try {
			if (value instanceof Timestamp) {
				return ((Timestamp) value).toLocalDateTime();
			} else if (value instanceof java.sql.Date) {
				return ((java.sql.Date) value).toLocalDate().atStartOfDay();
			} else if (value instanceof java.util.Date) {
				return new Timestamp(((java.util.Date) value).getTime()).toLocalDateTime();
			} else if (value instanceof LocalDateTime) {
				return (LocalDateTime) value;
			} else {
				String strValue = value.toString();
				return LocalDateTime.parse(strValue.replace(' ', 'T'));
			}
		} catch (Exception e) {
			return null;
		}
	}
}