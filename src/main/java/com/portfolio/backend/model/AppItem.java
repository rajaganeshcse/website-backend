package com.portfolio.backend.model;

import java.util.ArrayList;
import java.util.List;

public class AppItem extends BaseEntity {

  public String title = "";
  public String description = "";
  public String dashboard_url = "";
  public String github_url = "";
  public String playstore_url = "";
  public String cover_image_path = "";
  public List<String> screenshot_paths = new ArrayList<>();
  public String apk_path = "";
  public String apk_name = "";
  public int order = 0;
}
