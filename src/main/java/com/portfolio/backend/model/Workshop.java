package com.portfolio.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Workshop extends BaseEntity {

  public String title = "";
  public String organizer = "";
  public String date = "";
  public String description = "";
  public String link_url = "";
  public List<String> image_paths = new ArrayList<>();
  public int order = 0;
}
