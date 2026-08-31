package com.tbm.recruitment.identity.security;

import com.tbm.recruitment.identity.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

public final class SecurityResponseWriter {

  private SecurityResponseWriter() {}

  public static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {

    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    String body =
        """
                {"code":%d,"message":"%s","result":null}
                """
            .formatted(errorCode.getCode(), errorCode.getMessage())
            .trim();

    response.getWriter().write(body);
  }
}
