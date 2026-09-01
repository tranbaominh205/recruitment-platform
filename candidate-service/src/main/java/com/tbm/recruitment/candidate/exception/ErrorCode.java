package com.tbm.recruitment.candidate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  INVALID_REQUEST(2001, "Invalid request", HttpStatus.BAD_REQUEST),

  PROFILE_ALREADY_EXISTS(2002, "Candidate profile already exists", HttpStatus.CONFLICT),

  PROFILE_NOT_FOUND(2003, "Candidate profile not found", HttpStatus.NOT_FOUND),

  UNAUTHENTICATED(2004, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  FORBIDDEN(2005, "Forbidden", HttpStatus.FORBIDDEN),

  INTERNAL_SERVER_ERROR(2999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
