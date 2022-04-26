package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/**
 * Represents the display mode for a program.
 *
 * <p>For example, a {@code Program} may be fully public, hidden from the public index view, or
 * completely inaccessible.
 */
public enum DisplayMode {
  // The following modes are mutually exclusive.
  // The program should be fully visible.
  PUBLIC,
  // The program should not appear in the applicant's index screen.
  HIDDEN_IN_INDEX;

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }
}
