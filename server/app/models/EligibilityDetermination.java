package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/** Represents the eligibility determination status of an application. */
public enum EligibilityDetermination {
  // The application is eligible for this program.
  ELIGIBLE,
  // The application is ineligible for this program.
  INELIGIBLE,
  // Default eligibility determination used for existing applications
  // submitted before pre-compute eligibility feature were implemented.
  NOT_COMPUTED,
  // The program itself does not have any eligibility criteria.
  NO_ELIGIBILITY_CRITERIA;

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }
}
