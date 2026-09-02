package com.tbm.recruitment.recruitment.exception;

import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
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

    return buildErrorResponse(ErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {

    return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ApiResponse<Void>> buildErrorResponse(ErrorCode errorCode) {

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }
}
