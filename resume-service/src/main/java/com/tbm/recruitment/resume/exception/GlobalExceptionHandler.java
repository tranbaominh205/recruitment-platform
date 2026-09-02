package com.tbm.recruitment.resume.exception;

import com.tbm.recruitment.resume.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

    ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {

    ErrorCode errorCode = ErrorCode.INVALID_FILE;

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {

    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }
}
