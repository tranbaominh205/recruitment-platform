package com.tbm.recruitment.recruitment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  INVALID_REQUEST(6001, "Invalid request", HttpStatus.BAD_REQUEST),

  UNAUTHENTICATED(6002, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  FORBIDDEN(6003, "Forbidden", HttpStatus.FORBIDDEN),

  CANDIDATE_PROFILE_NOT_FOUND(6004, "Candidate profile not found", HttpStatus.NOT_FOUND),

  RESUME_NOT_FOUND(6005, "Resume not found", HttpStatus.NOT_FOUND),

  JOB_NOT_AVAILABLE(6006, "Job is not available for application", HttpStatus.CONFLICT),

  CANDIDATE_SERVICE_UNAVAILABLE(
      6007, "Candidate service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

  RESUME_SERVICE_UNAVAILABLE(6008, "Resume service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

  JOB_SERVICE_UNAVAILABLE(6009, "Job service unavailable", HttpStatus.SERVICE_UNAVAILABLE),

  APPLICATION_NOT_FOUND(6010, "Application not found", HttpStatus.NOT_FOUND),

  JOB_NOT_FOUND(6011, "Job not found", HttpStatus.NOT_FOUND),

  INTERNAL_SERVER_ERROR(6999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus status;

  ErrorCode(int code, String message, HttpStatus status) {

    this.code = code;
    this.message = message;
    this.status = status;
  }
}
