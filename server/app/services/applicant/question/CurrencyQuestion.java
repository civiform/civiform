package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.Currency;
import services.applicant.ValidationErrorMessage;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a currency question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class CurrencyQuestion extends QuestionImpl {

  // Stores the value, loading and caching it on first access.
  private Optional<Optional<Currency>> currencyCache;

  public CurrencyQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    this.currencyCache = Optional.empty();
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.CURRENCY);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getCurrencyPath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // TODO(#1944): Validate that the provided currency is a valid number. Presently,
    // this is only implemented in client-side validation.
    return ImmutableMap.of();
  }

  public Optional<Currency> getValue() {
    if (currencyCache.isEmpty()) {
      currencyCache =
          Optional.of(applicantQuestion.getApplicantData().readCurrency(getCurrencyPath()));
    }

    return currencyCache.get();
  }

  public CurrencyQuestionDefinition getQuestionDefinition() {
    return (CurrencyQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getCurrencyPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.CURRENCY_CENTS);
  }

  /**
   * Returns the currency value as decimal string with at least 1 dollars digit and always 2 cents
   * digits.
   *
   * <p>Returns "-" if there is no answer.
   */
  @Override
  public String getAnswerString() {
    return getValue().map(value -> value.getDollarsString()).orElse("-");
  }
}
