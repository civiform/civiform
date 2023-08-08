package models;

import com.google.common.collect.ImmutableMap;
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

  private static ImmutableMap<LifecycleStage, String> descriptions =
      ImmutableMap.of(
          LifecycleStage.DRAFT, "NOT_SUBMITTED",
          LifecycleStage.ACTIVE, "CURRENT",
          LifecycleStage.OBSOLETE, "OBSOLETE",
          LifecycleStage.DELETED, "DELETED");

  LifecycleStage(String stage) {
    this.stage = stage;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.stage;
  }

  /**
   * Returns a string representing the submission status of an application. The returned string is
   * independent of any configured program-specific status values. Used for API exports.
   */
  public String getDescription() {
    return descriptions.getOrDefault(this, "UNKNOWN");
  }
}
