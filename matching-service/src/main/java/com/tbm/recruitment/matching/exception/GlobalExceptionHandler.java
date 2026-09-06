package com.tbm.recruitment.matching.exception;

import com.tbm.recruitment.matching.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
    ErrorCode code = ex.getErrorCode();
    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }

  @ExceptionHandler(DownstreamServiceException.class)
  public ResponseEntity<ApiResponse<Object>> handleDownstream(DownstreamServiceException ex) {
    log.warn(
        "Downstream service call failed: service={}, status={}",
        ex.getServiceName(),
        ex.getHttpStatus());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            ApiResponse.error(
                ErrorCode.DEPENDENCY_UNAVAILABLE.getCode(),
                ErrorCode.DEPENDENCY_UNAVAILABLE.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest req) {
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(
            ApiResponse.error(
                ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleAll(Exception ex) {
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(
            ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }
}
