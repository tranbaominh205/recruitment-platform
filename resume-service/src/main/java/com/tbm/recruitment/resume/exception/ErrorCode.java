package com.tbm.recruitment.resume.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  INVALID_REQUEST(5001, "Invalid request", HttpStatus.BAD_REQUEST),

  INVALID_FILE(5002, "Invalid resume file", HttpStatus.BAD_REQUEST),

  UNAUTHENTICATED(5003, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  FORBIDDEN(5004, "Forbidden", HttpStatus.FORBIDDEN),

  STORAGE_ERROR(5005, "Resume storage error", HttpStatus.INTERNAL_SERVER_ERROR),

  INTERNAL_SERVER_ERROR(5999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
