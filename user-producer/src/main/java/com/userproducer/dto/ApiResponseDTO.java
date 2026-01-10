package com.userproducer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {

	private String status;
	private T data;
	private String message;
	private Integer statusCode;

	public static <T> ApiResponseDTO<T> success(T data) {
		return new ApiResponseDTO<>("OK", data, "Success", 200);
	}

	public static <T> ApiResponseDTO<T> success(T data, String message) {
		return new ApiResponseDTO<>("OK", data, message, 200);
	}

	public static <T> ApiResponseDTO<T> error(String message, Integer statusCode) {
		return new ApiResponseDTO<>("ERROR", null, message, statusCode);
	}
}
