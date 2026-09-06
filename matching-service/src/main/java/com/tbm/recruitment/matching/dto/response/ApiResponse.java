package com.tbm.recruitment.matching.dto.response;

public record ApiResponse<T>(int code, String message, T result) {

  public static <T> ApiResponse<T> success(T result) {
    return new ApiResponse<>(1000, "Success", result);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
