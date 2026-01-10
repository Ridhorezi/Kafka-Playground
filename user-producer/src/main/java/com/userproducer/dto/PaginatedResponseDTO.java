package com.userproducer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponseDTO<T> {
    private String status;
    private List<T> data;
    private PaginationInfo pagination;
    private String message;
    private Integer statusCode;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private Integer limit;
        private Integer offset;
        private Integer nextOffset;
        private Integer totalPage;
    }
    
    public static <T> PaginatedResponseDTO<T> success(List<T> data, PaginationInfo pagination) {
        return new PaginatedResponseDTO<>("OK", data, pagination, "Success", 200);
    }
    
    public static <T> PaginatedResponseDTO<T> success(List<T> data, PaginationInfo pagination, String message) {
        return new PaginatedResponseDTO<>("OK", data, pagination, message, 200);
    }
}