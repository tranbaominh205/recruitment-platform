package com.tbm.recruitment.candidate.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

  private int code;

  private String message;

  private T result;
}
