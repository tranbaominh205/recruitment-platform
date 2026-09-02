package com.tbm.recruitment.resume.exception;

import com.tbm.recruitment.resume.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

  @ExceptionHandler(MissingServletRequestPartException.class)
  ResponseEntity<ApiResponse<Void>> handleMissingRequestPart(
      MissingServletRequestPartException exception) {

    return buildErrorResponse(ErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiResponse<Void>> handleArgumentTypeMismatch(
      MethodArgumentTypeMismatchException exception) {

    return buildErrorResponse(ErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {

    return buildErrorResponse(ErrorCode.INVALID_FILE);
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
