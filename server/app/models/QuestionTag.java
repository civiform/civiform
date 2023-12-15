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
  PRIMARY_APPLICANT_EMAIL,
  PRIMARY_APPLICANT_NAME,
  PRIMARY_APPLICANT_PHONE,
  PRIMARY_APPLICANT_DOB,
  PRIMARY_EMERGENCY_CONTACT_NAME; // This one doesn't actually do anything

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }

  public static ImmutableList<QuestionTag> getPrimaryApplicantInfoTags() {
    return ImmutableList.of(
        QuestionTag.PRIMARY_APPLICANT_DOB,
        QuestionTag.PRIMARY_APPLICANT_EMAIL,
        QuestionTag.PRIMARY_APPLICANT_NAME,
        QuestionTag.PRIMARY_APPLICANT_PHONE,
        QuestionTag.PRIMARY_EMERGENCY_CONTACT_NAME);
  }
}
