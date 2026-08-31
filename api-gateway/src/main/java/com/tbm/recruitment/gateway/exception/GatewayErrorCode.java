package com.tbm.recruitment.gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GatewayErrorCode {
  UNAUTHENTICATED(9001, "Unauthenticated", HttpStatus.UNAUTHORIZED),

  IDENTITY_SERVICE_UNAVAILABLE(
      9002, "Identity service unavailable", HttpStatus.SERVICE_UNAVAILABLE);

  private final int code;
  private final String message;
  private final HttpStatus httpStatus;

  GatewayErrorCode(int code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }
}
