package com.portfolio.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Project extends BaseEntity {

  public String title = "";
  public String description = "";
  public String tech_stack = "";
  public String github_url = "";
  public String live_url = "";
  public String video_url = "";
  public List<String> image_paths = new ArrayList<>();
  public int order = 0;
}
