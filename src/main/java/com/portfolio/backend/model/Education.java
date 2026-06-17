package com.portfolio.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Education extends BaseEntity {

  public String degree = "";
  public String institution = "";
  public String year = "";
  public String score = "";
  public String result_pdf_path = "";
  public String result_pdf_name = "";
  public List<EducationDocument> documents = new ArrayList<>();
  public int order = 0;
}
