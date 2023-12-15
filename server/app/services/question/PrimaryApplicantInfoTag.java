package services.question;

import com.google.common.collect.ImmutableList;
import models.QuestionTag;
import services.question.types.QuestionType;

/**
 * Defines tags that can be applied to a question to denote the answer is a primary piece
 * of information about the applicant. These are 1:1 with particular QuestionTags.
 * To add a new value: 
 *   - Add a new QuestionTag 
 *   - Add a new value to the enum here 
 *   - Update QuestionForm to add a function to handle the fieldName in the enum 
 *   - Implement the logic for saving the data on application submission
 */
public enum PrimaryApplicantInfoTag {
  APPLICANT_DOB(
      QuestionTag.PRIMARY_APPLICANT_DOB,
      QuestionType.DATE,
      "primaryApplicantInfoDob",
      "Setting this property will allow the answer to be pre-populated with the"
          + " applicant's date of birth if a TI created this applicant."),
  APPLICANT_EMAIL(
      QuestionTag.PRIMARY_APPLICANT_EMAIL,
      QuestionType.EMAIL,
      "primaryApplicantInfoEmail",
      "Setting this property will allow the email address collected from this"
          + " question to be used to email status updates to guest applicants, as well as make the"
          + " application searchable by this address."),
  APPLICANT_NAME(
      QuestionTag.PRIMARY_APPLICANT_NAME,
      QuestionType.NAME,
      "primaryApplicantInfoName",
      "Setting this property will allow CiviForm to use the name to identify the"
          + " user and their application in the UI, as well as make the application searchable by"
          + " name."),
  APPLICANT_PHONE(
      QuestionTag.PRIMARY_APPLICANT_PHONE,
      QuestionType.PHONE,
      "primaryApplicantInfoPhone",
      "Setting this property will make the application searchable by phone"
          + " number."),
  EMERGENCY_CONTACT_NAME(
      QuestionTag.PRIMARY_EMERGENCY_CONTACT_NAME,
      QuestionType.NAME,
      "primaryEmergencyContactName",
      "Setting this property will record this name as the applicant's emergency contact.");

  private final QuestionTag tag;
  private final QuestionType type;
  private final String fieldName;
  private final String description;

  PrimaryApplicantInfoTag(QuestionTag tag, QuestionType type, String fieldName, String description) {
    this.tag = tag;
    this.type = type;
    this.fieldName = fieldName;
    this.description = description;
  }

  public QuestionTag getQuestionTag() {
    return this.tag;
  }

  public QuestionType getQuestionType() {
    return this.type;
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public String getDescription() {
    return this.description;
  }

  public static ImmutableList<QuestionType> getAllQuestionTypes() {
    return ImmutableList.copyOf(PrimaryApplicantInfoTag.values()).stream()
        .map(t -> t.getQuestionType())
        .collect(ImmutableList.toImmutableList());
  }

  public static ImmutableList<PrimaryApplicantInfoTag> getAllTagsForQuestionType(
      QuestionType questionType) {
    return ImmutableList.copyOf(PrimaryApplicantInfoTag.values()).stream()
        .filter(t -> t.getQuestionType().equals(questionType))
        .collect(ImmutableList.toImmutableList());
  }
}
