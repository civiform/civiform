package services.program;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/**
 * ProgramType for a Program. Most Programs are a regular DEFAULT program. A COMMON_INTAKE_FORM
 * Program is an intake screener for other programs.
 */
public enum ProgramType {
  DEFAULT("default"),
  COMMON_INTAKE_FORM("common_intake_form"),
  EXTERNAL_PROGRAM("external_program");

  private final String dbValue;

  ProgramType(String dbValue) {
    this.dbValue = dbValue;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return dbValue;
  }

  public static ProgramType fromValue(String value) {
    if (value.equals("default")) {
      return DEFAULT;
    } else if (value.equals("common_intake_form")) {
      return COMMON_INTAKE_FORM;
    } else if (value.equals("external_program")) {
      return EXTERNAL_PROGRAM;
    }
    return DEFAULT;
  }
}
