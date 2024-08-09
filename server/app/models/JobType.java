package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/** Represents the job type for the durable jobs. */
public enum JobType {
  /** Runs via Akka scheduler (the current behavior). Existing jobs will be assigned this. */
  RECURRING,
  /**
   * Runs a job once each time the application is started, prior to the site being accessible to
   * users.
   */
  RUN_ON_EACH_STARTUP,

  /** Runs a job once at application startup, prior to the site being accessible to users. */
  RUN_ONCE;

  @DbEnumValue(storage = DbEnumType.VARCHAR, length = 32)
  public String getValue() {
    return this.name();
  }
}
