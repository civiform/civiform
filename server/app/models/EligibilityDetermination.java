package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

public enum EligibilityDetermination {
  ELIGIBLE,
  INELIGIBLE,
  NOT_COMPUTED,
  NO_ELIGIBILITY_CRITERIA;

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }
}
