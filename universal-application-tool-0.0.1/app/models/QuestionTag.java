package models;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

/**
 * Tags related to questions. The database allows a list of tags, and this would be a good place to
 * put tags related to questions that are not needed for rendering.
 */
public enum QuestionTag {
  // The following three tags are mutually exclusive.
  // This question should be exported in the demographic export csv.
  DEMOGRAPHIC,
  // This question should be hashed in the demographic export csv.
  DEMOGRAPHIC_PII,
  // This question should not be exported in the demographic export csv.
  NON_DEMOGRAPHIC;

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }
}
