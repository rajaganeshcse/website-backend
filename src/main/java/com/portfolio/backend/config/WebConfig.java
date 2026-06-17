package com.portfolio.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final AppProperties properties;

  public WebConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String mediaLocation = properties.getMediaRootAbsolutePath().toUri().toString();
    if (!mediaLocation.endsWith("/")) {
      mediaLocation = mediaLocation + "/";
    }

    registry.addResourceHandler("/media/**")
        .addResourceLocations(mediaLocation);
  }
}
