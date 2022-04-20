package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents an enumerator question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class EnumeratorQuestion implements Question {

  private final ApplicantQuestion applicantQuestion;

  public EnumeratorQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    // Not intended to return the leaf question paths.
    return ImmutableList.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in an enumerator question.
    return ImmutableSet.of();
  }

  /** No blank values are allowed. No duplicated entity names are allowed. */
  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<ValidationErrorMessage> errorsBuilder = ImmutableSet.builder();
    ImmutableList<String> entityNames = getEntityNames();
    if (entityNames.stream().anyMatch(String::isBlank)) {
      errorsBuilder.add(
          ValidationErrorMessage.create(MessageKey.ENUMERATOR_VALIDATION_ENTITY_REQUIRED));
    }
    if (entityNames.stream().collect(ImmutableSet.toImmutableSet()).size() != entityNames.size()) {
      errorsBuilder.add(
          ValidationErrorMessage.create(MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME));
    }
    return errorsBuilder.build();
  }

  public ValidationErrorMessage getQuestionErrorMessage() {
    return ValidationErrorMessage.create(MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME);
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.ENUMERATOR)) {
      throw new RuntimeException(
          String.format(
              "Question is not a ENUMERATOR question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public EnumeratorQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (EnumeratorQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  /** This is answered if the the path to the enumerator question answer array exists. */
  @Override
  public boolean isAnswered() {
    return applicantQuestion
        .getApplicantData()
        .hasPath(applicantQuestion.getContextualizedPath().atIndex(0));
  }

  /** Return the repeated entity names associated with this enumerator question. */
  public ImmutableList<String> getEntityNames() {
    return applicantQuestion
        .getApplicantData()
        .readRepeatedEntities(applicantQuestion.getContextualizedPath());
  }

  /**
   * Get the localized admin-configurable entity type this enumerator represents. Examples: "car",
   * "child", "job", "household member".
   */
  public String getEntityType() {
    return getQuestionDefinition()
        .getEntityType()
        .getOrDefault(applicantQuestion.getApplicantData().preferredLocale());
  }

  @Override
  public String getAnswerString() {
    return Joiner.on("\n").join(getEntityNames());
  }
}
