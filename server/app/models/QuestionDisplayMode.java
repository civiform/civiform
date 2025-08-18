package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/** Represents the display mode for a {@link QuestionModel} */
public enum QuestionDisplayMode {
  VISIBLE("Visible"),
  HIDDEN("Hidden");

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }

  private final String displayMode;

  QuestionDisplayMode(String displayMode) {
    this.displayMode = displayMode;
  }

  public String getLabel() {
    return this.displayMode;
  }
}
