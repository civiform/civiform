package services.question.types;

import com.google.common.collect.ImmutableList;
import models.QuestionTag;

/**
 * Defines types of actions that can be applied to a question. To add a new action: - Add a new
 * QuestionTag for the action - Add a new value to the enum here - Update QuestionForm to add a
 * function to handle the fieldName in the enum - Implement the logic for the action where needed
 */
public enum AnswerActionType {
  APPLICANT_INFO_DOB(
      QuestionTag.ACTION_APPLICANT_INFO_DOB,
      QuestionType.DATE,
      "actionApplicantInfoDob",
      "Setting this as an actionable question will allow the answer to be pre-populated with the"
          + " applicant's date of birth if a TI created this applicant."),
  APPLICANT_INFO_EMAIL(
      QuestionTag.ACTION_APPLICANT_INFO_EMAIL,
      QuestionType.EMAIL,
      "actionApplicantInfoEmail",
      "Setting this as an actionable question will allow the email address collected from this"
          + " question to be used to email status updates to guest applicants, as well as make the"
          + " application searchable by this address."),
  APPLICANT_INFO_NAME(
      QuestionTag.ACTION_APPLICANT_INFO_NAME,
      QuestionType.NAME,
      "actionApplicantInfoName",
      "Setting this as an actionable question will allow CiviForm to use the name to identify the"
          + " user and their application in the UI, as well as make the application searchable by"
          + " name."),
  APPLICANT_INFO_PHONE(
      QuestionTag.ACTION_APPLICANT_INFO_PHONE,
      QuestionType.PHONE,
      "actionApplicantInfoPhone",
      "Setting this as an actionable question will make the application searchable by phone"
          + " number."),
  DIFFERENT_NAME_ACTION(
      QuestionTag.ACTION_DIFFERENT_NAME_ACTION,
      QuestionType.NAME,
      "actionDifferent",
      "This is a totally different action.");

  private final QuestionTag tag;
  private final QuestionType type;
  private final String fieldName;
  private final String description;

  AnswerActionType(QuestionTag tag, QuestionType type, String fieldName, String description) {
    this.tag = tag;
    this.type = type;
    this.fieldName = fieldName;
    this.description = description;
  }

  public QuestionTag getTag() {
    return this.tag;
  }

  public QuestionType getType() {
    return this.type;
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public String getDescription() {
    return this.description;
  }

  public static ImmutableList<QuestionType> getActionableTypes() {
    return ImmutableList.copyOf(AnswerActionType.values()).stream()
        .map(t -> t.getType())
        .collect(ImmutableList.toImmutableList());
  }

  public static ImmutableList<AnswerActionType> getActionsForQuestionType(
      QuestionType questionType) {
    return ImmutableList.copyOf(AnswerActionType.values()).stream()
        .filter(t -> t.getType().equals(questionType))
        .collect(ImmutableList.toImmutableList());
  }
}
