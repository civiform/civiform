package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.CurrencyQuestionDefinition;

/**
 * Represents a currency question in the context of a specific applicant.
 *
 * Currency is handled as USD cents.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class CurrencyQuestion implements PresentsErrors {
  private final ApplicantQuestion applicantQuestion;
  private Optional<Long> centsValue;

  public CurrencyQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getCurrencyPath());
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // No errors are possible currently.
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a currency question.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getCurrencyPath());
  }

  public Optional<Long> getCurrencyValue() {
    if (centsValue != null) {
      return centsValue;
    }

    centsValue = applicantQuestion.getApplicantData().readLong(getCurrencyPath());

    return centsValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.CURRENCY)) {
      throw new RuntimeException(
          String.format(
              "Question is not a CURRENCY question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public CurrencyQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (CurrencyQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getCurrencyPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.NUMBER);
  }

  @Override
  public String getAnswerString() {
    return getCurrencyValue().map(Object::toString).orElse("-");
  }
}
