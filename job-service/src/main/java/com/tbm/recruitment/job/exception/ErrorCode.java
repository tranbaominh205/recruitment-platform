package com.tbm.recruitment.job.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  INVALID_REQUEST(4001, "Invalid request", HttpStatus.BAD_REQUEST),

  INVALID_SALARY_RANGE(
      4002,
      "Maximum salary must be greater than or equal to minimum salary",
      HttpStatus.BAD_REQUEST),

  COMPANY_NOT_FOUND(4003, "Company not found", HttpStatus.NOT_FOUND),

  UNAUTHENTICATED(4004, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  FORBIDDEN(4005, "Forbidden", HttpStatus.FORBIDDEN),

  EMPLOYER_SERVICE_UNAVAILABLE(
      4006, "Employer service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

  JOB_NOT_FOUND(4007, "Job not found", HttpStatus.NOT_FOUND),

  INVALID_JOB_STATUS(4008, "Invalid job status transition", HttpStatus.CONFLICT),

  INTERNAL_SERVER_ERROR(4999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
