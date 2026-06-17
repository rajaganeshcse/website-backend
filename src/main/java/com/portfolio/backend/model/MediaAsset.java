package com.portfolio.backend.model;

import java.util.Date;

public class MediaAsset extends BaseEntity {

  public String owner_model = "";
  public String owner_id = "";
  public String stored_path = "";
  public String source_field = "";
  public String original_name = "";
  public String media_kind = "";
  public Date expires_at;
  public Date deleted_at;
  public String cleanup_status = "scheduled";
  public String cleanup_error = "";
}
