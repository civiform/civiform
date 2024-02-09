package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import models.QuestionTag;
import services.question.types.QuestionType;

/**
 * Defines tags that can be applied to a question to denote the answer is a primary piece of
 * information about the applicant. These are 1:1 with {@link QuestionTag}s that start with
 * `PRIMARY_APPLICANT`. See the javadoc for the constructor for details about the fields in the
 * enum.
 */
public enum PrimaryApplicantInfoTag {
  APPLICANT_DOB(
      QuestionTag.PRIMARY_APPLICANT_DOB,
      QuestionType.DATE,
      "primaryApplicantDob",
      "Set answer as applicant date of birth",
      "If this is turned on, the answer will be pre-filled with the applicant's date of birth that"
          + " the Trusted Intermediary input during client account setup."),
  APPLICANT_EMAIL(
      QuestionTag.PRIMARY_APPLICANT_EMAIL,
      QuestionType.EMAIL,
      "primaryApplicantEmail",
      "Set answer as applicant email address",
      "If this is turned on, the email address collected from this question will be used to email"
          + " updates to guest applicants. Admins can also search for an application with this"
          + " email address."),
  APPLICANT_NAME(
      QuestionTag.PRIMARY_APPLICANT_NAME,
      QuestionType.NAME,
      "primaryApplicantName",
      "Set answer as applicant Name",
      "If this is turned on, the name collected from this question will be used to identify an"
          + " applicant and search for their application."),
  APPLICANT_PHONE(
      QuestionTag.PRIMARY_APPLICANT_PHONE,
      QuestionType.PHONE,
      "primaryApplicantPhone",
      "Set answer as applicant phone number",
      "If this is turned on, admins can search for an application by this collected phone number.");

  private final QuestionTag tag;
  private final QuestionType type;
  private final String fieldName;
  private final String displayName;
  private final String description;

  /**
   * This contains information about a tag that can be applied to a question, marking it as a piece
   * of primary applicant information.
   *
   * @param tag The QuestionTag associated with this PrimaryApplicantInfoTag
   * @param type The QuestionType this PrimaryApplicantInfoTag can be applied to
   * @param fieldName The name of the field used in the QuestionForm for setting the tag for a
   *     question
   * @param displayName The string used in the question create/edit UI as a label for the toggle
   * @param description The string used in the question create/edit UI to describe what this tag is
   *     for. Appears above the toggle in smaller text.
   */
  PrimaryApplicantInfoTag(
      QuestionTag tag,
      QuestionType type,
      String fieldName,
      String displayName,
      String description) {
    this.tag = tag;
    this.type = type;
    this.fieldName = fieldName;
    this.displayName = displayName;
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

  public String getDisplayName() {
    return this.displayName;
  }

  public String getDescription() {
    return this.description;
  }

  public static ImmutableSet<PrimaryApplicantInfoTag> getAllTagsForQuestionType(
      QuestionType questionType) {
    return ImmutableList.copyOf(PrimaryApplicantInfoTag.values()).stream()
        .filter(t -> t.getQuestionType().equals(questionType))
        .collect(ImmutableSet.toImmutableSet());
  }

  public static ImmutableSet<QuestionType> getAllQuestionTypes() {
    return ImmutableList.copyOf(PrimaryApplicantInfoTag.values()).stream()
        .map(t -> t.getQuestionType())
        .collect(ImmutableSet.toImmutableSet());
  }
}
