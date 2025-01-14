package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;

/**
 * Represents a single-select question in the context of a specific applicant.
 *
 * <p>All single-select question types share this class, e.g. dropdown and radio button questions.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class SingleSelectQuestion extends AbstractQuestion {

  // Stores the value, loading and caching it on first access.
  private Optional<Optional<LocalizedQuestionOption>> selectedOptionValueCache;

  SingleSelectQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    selectedOptionValueCache = Optional.empty();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getSelectionPath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // Only one selection is possible - there is no admin-configured validation.
    return ImmutableMap.of();
  }

  public boolean hasValue() {
    return getSelectedOptionValue().isPresent();
  }

  /** Get the ID of the selected option. */
  public Optional<Long> getSelectedOptionId() {
    return applicantQuestion.getApplicantData().readLong(getSelectionPath());
  }

  /** Get the selected option in the applicant's preferred locale. */
  public Optional<LocalizedQuestionOption> getSelectedOptionValue() {
    if (selectedOptionValueCache.isEmpty()) {
      selectedOptionValueCache =
          Optional.of(
              getSelectedOptionValue(applicantQuestion.getApplicantData().preferredLocale()));
    }
    return selectedOptionValueCache.get();
  }

  /** Get the selected option in the specified locale. */
  public Optional<LocalizedQuestionOption> getSelectedOptionValue(Locale locale) {
    Optional<Long> selectedOptionIdOpt = getSelectedOptionId();
    if (selectedOptionIdOpt.isEmpty()) {
      return Optional.empty();
    }

    Long selectedOptionId = selectedOptionIdOpt.get();
    return getOptions(locale).stream()
        .filter(option -> selectedOptionId == option.id())
        .findFirst();
  }

  public Optional<String> getSelectedOptionAdminName() {
    Optional<Long> maybeSelectedOptionId = getSelectedOptionId();
    if (maybeSelectedOptionId.isEmpty()) {
      return Optional.empty();
    }

    Long selectedOptionId = maybeSelectedOptionId.get();
    return getQuestionDefinition().getOptions().stream()
        .filter(option -> selectedOptionId == option.id())
        .findFirst()
        .map(QuestionOption::adminName);
  }

  public MultiOptionQuestionDefinition getQuestionDefinition() {
    return (MultiOptionQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getSelectionPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.SELECTION);
  }

  public boolean optionIsSelected(LocalizedQuestionOption option) {
    return getSelectedOptionValue().isPresent() && getSelectedOptionValue().get().equals(option);
  }

  /** Get options in the applicant's preferred locale. */
  public ImmutableList<LocalizedQuestionOption> getOptions() {
    return getOptions(applicantQuestion.getApplicantData().preferredLocale());
  }

  /** Get options in the specified locale. */
  public ImmutableList<LocalizedQuestionOption> getOptions(Locale locale) {
    return getQuestionDefinition().getOptionsForLocaleOrDefault(locale);
  }

  @Override
  public String getAnswerString() {
    return getSelectedOptionValue()
        .map(LocalizedQuestionOption::optionText)
        .orElse(getDefaultAnswerString());
  }
}
