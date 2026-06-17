package com.portfolio.backend.model;

import java.util.Date;

public class MessageItem extends BaseEntity {

  public String name = "";
  public String email = "";
  public String message = "";
  public Date sent_at;
  public boolean is_read = false;
}
