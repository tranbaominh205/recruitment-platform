package com.tbm.recruitment.employer.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  INVALID_REQUEST(3001, "Invalid request", HttpStatus.BAD_REQUEST),

  COMPANY_ALREADY_EXISTS(3002, "Company already exists", HttpStatus.CONFLICT),

  COMPANY_NOT_FOUND(3003, "Company not found", HttpStatus.NOT_FOUND),

  UNAUTHENTICATED(3004, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  FORBIDDEN(3005, "Forbidden", HttpStatus.FORBIDDEN),

  INTERNAL_SERVER_ERROR(3999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
