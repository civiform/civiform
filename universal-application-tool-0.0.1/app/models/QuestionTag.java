package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

public enum QuestionTag {
  DEMOGRAPHIC("demographic"),
  DEMOGRAPHIC_PII("demographic_pii");

  private final String tag;

  QuestionTag(String tag) {
    this.tag = tag;
  }

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.tag;
  }
}
