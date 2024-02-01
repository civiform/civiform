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
      "Applicant Date Of Birth",
      "Setting this property will allow the answer to be pre-populated with the"
          + " applicant's date of birth if a TI created this applicant."),
  APPLICANT_EMAIL(
      QuestionTag.PRIMARY_APPLICANT_EMAIL,
      QuestionType.EMAIL,
      "primaryApplicantEmail",
      "Applicant Email Address",
      "Setting this property will allow the email address collected from this"
          + " question to be used to email status updates to guest applicants, as well as make the"
          + " application searchable by this address."),
  APPLICANT_NAME(
      QuestionTag.PRIMARY_APPLICANT_NAME,
      QuestionType.NAME,
      "primaryApplicantName",
      "Applicant Name",
      "Setting this property will allow CiviForm to use the name to identify the"
          + " user and their application in the UI, as well as make the application searchable by"
          + " name."),
  APPLICANT_PHONE(
      QuestionTag.PRIMARY_APPLICANT_PHONE,
      QuestionType.PHONE,
      "primaryApplicantPhone",
      "Applicant Phone Number",
      "Setting this property will make the application searchable by phone number.");

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
   * @param displayName The string used in the question create/edit UI as a title for the toggle
   * @param description The string used in the question create/edit UI to describe what this tag is
   *     for
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
