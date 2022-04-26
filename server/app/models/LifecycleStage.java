package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/**
 * Represents a stage in a model's lifecycle.
 *
 * <p>For example, {@code Version}s begin as drafts, are published and become active, and become
 * obsolete once the next version is published.
 */
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
