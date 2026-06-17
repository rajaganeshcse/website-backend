package com.portfolio.backend.model;

import java.util.Date;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.bson.BsonType;

public abstract class BaseEntity {

  @BsonId
  @BsonRepresentation(BsonType.OBJECT_ID)
  @BsonProperty("_id")
  public String id;
  public Date createdAt;
  public Date updatedAt;
}
