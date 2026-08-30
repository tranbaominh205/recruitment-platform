package com.tbm.recruitment.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  INVALID_REQUEST(1001, "Invalid request", HttpStatus.BAD_REQUEST),

  EMAIL_ALREADY_EXISTS(1002, "Email already exists", HttpStatus.BAD_REQUEST),

  ADMIN_REGISTRATION_NOT_ALLOWED(1003, "Admin registration is not allowed", HttpStatus.BAD_REQUEST),

  ACCOUNT_NOT_FOUND(1004, "Account not found", HttpStatus.NOT_FOUND),

  INVALID_CREDENTIALS(1005, "Invalid email or password", HttpStatus.UNAUTHORIZED),
  UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  UNAUTHORIZED(1007, "Unauthorized", HttpStatus.FORBIDDEN),

  ACCOUNT_DISABLED(1008, "Account is disabled", HttpStatus.FORBIDDEN),

  INTERNAL_SERVER_ERROR(9999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
