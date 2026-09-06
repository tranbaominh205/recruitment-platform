package com.tbm.recruitment.notification.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  UNAUTHENTICATED(8001, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  FORBIDDEN(8002, "Forbidden", HttpStatus.FORBIDDEN),

  CANDIDATE_NOT_FOUND(8003, "Candidate not found", HttpStatus.NOT_FOUND),

  CANDIDATE_SERVICE_UNAVAILABLE(
      8004, "Candidate service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

  INTERNAL_SERVER_ERROR(8999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
