package com.portfolio.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
    return build(exception.getStatusCode(), exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> handleUploadException(MaxUploadSizeExceededException exception) {
    return build(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException exception) {
    return build(HttpStatus.NOT_FOUND.value(), "Route not found: " + exception.getResourcePath());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception exception) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR.value(),
        exception.getMessage() == null || exception.getMessage().isBlank()
            ? "Internal server error"
            : exception.getMessage());
  }

  private ResponseEntity<Map<String, Object>> build(int statusCode, String detail) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("detail", detail);
    return ResponseEntity.status(statusCode).body(body);
  }
}
