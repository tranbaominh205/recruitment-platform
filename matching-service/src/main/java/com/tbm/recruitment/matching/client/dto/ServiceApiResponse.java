package com.tbm.recruitment.matching.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceApiResponse<T> {

  private int code;
  private String message;
  private T result;
}
