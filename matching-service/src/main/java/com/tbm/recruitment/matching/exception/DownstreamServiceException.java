package com.tbm.recruitment.matching.exception;

import lombok.Getter;

@Getter
public class DownstreamServiceException extends RuntimeException {

  private final String serviceName;
  private final int httpStatus;

  public DownstreamServiceException(String serviceName, int httpStatus, String message) {
    super(message);
    this.serviceName = serviceName;
    this.httpStatus = httpStatus;
  }
}
