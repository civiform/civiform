package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/**
 * Represents the display mode for a program.
 *
 * <p>For example, a {@code Program} may be fully public, hidden from the public index view, or
 * completely inaccessible.
 */
public enum NameSuffix {
  JR("Jr."),
  SR("Sr."),
  I("I"),
  II("II"),
  III("III"),
  IV("IV"),
  V("V");

  private final String abbreviation;

  NameSuffix(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.abbreviation;
  }
}
