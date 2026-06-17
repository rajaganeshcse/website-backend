package com.portfolio.backend.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

public final class MultipartRequestUtil {

  private MultipartRequestUtil() {
  }

  public static Map<String, String> fields(MultipartHttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    request.getParameterMap().forEach((key, value) -> fields.put(key, value != null && value.length > 0 ? value[0] : ""));
    return fields;
  }

  public static MultipartFile file(MultipartHttpServletRequest request, String field) {
    return request.getFile(field);
  }

  public static List<MultipartFile> files(MultipartHttpServletRequest request, String... fields) {
    return Arrays.stream(fields)
        .map(request::getFile)
        .filter(file -> file != null && !file.isEmpty())
        .toList();
  }

  public static boolean truthy(String value) {
    String normalized = value == null ? "" : value.trim().toLowerCase();
    return normalized.equals("1") || normalized.equals("true") || normalized.equals("yes") || normalized.equals("on");
  }
}
