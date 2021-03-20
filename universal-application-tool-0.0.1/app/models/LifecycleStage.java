package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

public enum LifecycleStage {
  DRAFT("draft"),
  ACTIVE("active"),
  OBSOLETE("obsolete"),
  DELETED("deleted");

  private final String stage;

  LifecycleStage(String stage) {
    this.stage = stage;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.stage;
  }
}
