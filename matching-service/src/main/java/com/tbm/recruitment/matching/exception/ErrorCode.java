package com.tbm.recruitment.matching.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),
  INVALID_REQUEST(7001, "Invalid request", HttpStatus.BAD_REQUEST),
  UNAUTHENTICATED(7002, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  FORBIDDEN(7003, "Forbidden", HttpStatus.FORBIDDEN),
  APPLICATION_NOT_FOUND(7004, "Application not found", HttpStatus.NOT_FOUND),
  MATCH_RESULT_NOT_FOUND(7005, "Match result not found", HttpStatus.NOT_FOUND),
  DEPENDENCY_UNAVAILABLE(7006, "Dependency unavailable", HttpStatus.SERVICE_UNAVAILABLE),
  INTERNAL_SERVER_ERROR(7999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
