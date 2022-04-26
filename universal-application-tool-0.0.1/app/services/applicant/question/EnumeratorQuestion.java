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
public class EnumeratorQuestion extends QuestionImpl {

  public EnumeratorQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.ENUMERATOR);
  }

  @Override
  public boolean isAnswered() {
    // This is answered if the the path to the enumerator question answer array exists.
    return applicantQuestion
        .getApplicantData()
        .hasPath(applicantQuestion.getContextualizedPath().atIndex(0));
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    // Not intended to return the leaf question paths.
    return ImmutableList.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // There are no inherent requirements in an enumerator question.
    return ImmutableSet.of();
  }

  /** No blank values are allowed. No duplicated entity names are allowed. */
  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
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

  public EnumeratorQuestionDefinition getQuestionDefinition() {
    return (EnumeratorQuestionDefinition) applicantQuestion.getQuestionDefinition();
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
