package services.program;

import io.ebean.annotation.DbEnumValue;
import io.ebean.annotation.DbEnumType;

/**
 * ProgramType for a Program. Most Programs are a regular PROGRAM. A
 * COMMON_INTAKE_FORM Program is an intake screener for other programs.
 */
public enum ProgramType {
    PROGRAM("PROGRAM"),
    COMMON_INTAKE_FORM("COMMON_INTAKE_FORM");

    private final String dbValue;

    ProgramType(String dbValue) {
        this.dbValue = dbValue;
    }

    @DbEnumValue(storage = DbEnumType.VARCHAR)
    public String getValue() {
        return dbValue;
    }
}
