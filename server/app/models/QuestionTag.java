package models;

import com.google.common.collect.ImmutableList;
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
  NON_DEMOGRAPHIC,
  // This question is a universal question, shown with a badge in the UI
  UNIVERSAL,
  ACTION_DIFFERENT_NAME_ACTION,
  ACTION_APPLICANT_INFO_EMAIL,
  ACTION_APPLICANT_INFO_NAME,
  ACTION_APPLICANT_INFO_PHONE,
  ACTION_APPLICANT_INFO_DOB;

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }

  public static ImmutableList<QuestionTag> getActionTags() {
    return ImmutableList.of(
        QuestionTag.ACTION_APPLICANT_INFO_DOB,
        QuestionTag.ACTION_APPLICANT_INFO_EMAIL,
        QuestionTag.ACTION_APPLICANT_INFO_NAME,
        QuestionTag.ACTION_APPLICANT_INFO_PHONE);
  }
}
