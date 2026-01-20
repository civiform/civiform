package services.program;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/**
 * ProgramType for a Program. Most Programs are a regular DEFAULT program. A PRE_SCREENER_FORM
 * Program is an intake screener for other programs.
 */
public enum ProgramType {
  DEFAULT("default"),
  PRE_SCREENER_FORM("common_intake_form"),
  EXTERNAL("external");

  private final String dbValue;

  ProgramType(String dbValue) {
    this.dbValue = dbValue;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return dbValue;
  }

  public static ProgramType fromValue(String programTypeValue) {
    for (ProgramType programType : values()) {
      if (programType.getValue().equals(programTypeValue)) {
        return programType;
      }
    }
    throw new IllegalArgumentException("Unknown ProgramType for: " + programTypeValue);
  }
}
