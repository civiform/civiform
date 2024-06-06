package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/** Represents a stage in an ApplicationStatus's lifecycle. */
public enum StatusLifecycleStage {
  ACTIVE("active"),
  OBSOLETE("obsolete");

  private final String stage;

  StatusLifecycleStage(String stage) {
    this.stage = stage;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.stage;
  }
}
