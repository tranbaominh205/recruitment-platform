package com.tbm.recruitment.employer.exception;

import com.tbm.recruitment.employer.dto.response.ApiResponse;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
    ErrorCode errorCode = exception.getErrorCode();

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException exception) {

    Map<String, String> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    error -> error.getField(),
                    error -> error.getDefaultMessage(),
                    (first, second) -> first));

    ApiResponse<Map<String, String>> response =
        ApiResponse.<Map<String, String>>builder()
            .code(ErrorCode.INVALID_REQUEST.getCode())
            .message(ErrorCode.INVALID_REQUEST.getMessage())
            .result(errors)
            .build();

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiResponse<Void>> handleUnreadableRequest() {
    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(ErrorCode.INVALID_REQUEST.getCode())
            .message(ErrorCode.INVALID_REQUEST.getMessage())
            .build();

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleException() {
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }
}
