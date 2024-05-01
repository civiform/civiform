package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.Currency;
import services.applicant.ValidationErrorMessage;
import services.question.types.CurrencyQuestionDefinition;

/**
 * Represents a currency question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class CurrencyQuestion extends Question {

  // Stores the value, loading and caching it on first access.
  private Optional<Optional<Currency>> currencyCache;

  CurrencyQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    this.currencyCache = Optional.empty();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getCurrencyPath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // When staging updates, the attempt to update ApplicantData would have failed to
    // convert to a currency and been noted as a failed update. We check for that here.
    if (applicantQuestion.getApplicantData().updateDidFailAt(getCurrencyPath())) {
      return ImmutableMap.of(
          getCurrencyPath(),
          ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.CURRENCY_VALIDATION_MISFORMATTED)));
    }
    return ImmutableMap.of();
  }

  public Optional<Currency> getCurrencyValue() {
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
    return getCurrencyValue().map(Currency::getDollarsString).orElse(getDefaultAnswerString());
  }
}
