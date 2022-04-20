package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;

/**
 * Represents a single-select question in the context of a specific applicant.
 *
 * <p>All single-select question types share this class, e.g. dropdown and radio button questions.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class SingleSelectQuestion implements Question {

  private final ApplicantQuestion applicantQuestion;
  // Stores the value, loading and caching it on first access.
  private Optional<Optional<LocalizedQuestionOption>> selectedOptionValueCache;

  public SingleSelectQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    selectedOptionValueCache = Optional.empty();
    assertQuestionType();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getSelectionPath());
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // Only one selection is possible - there is no admin-configured validation.
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // Only one selection is possible - there is no admin-configured validation.
    return ImmutableSet.of();
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

  public void assertQuestionType() {
    if (!applicantQuestion.getType().isMultiOptionType()) {
      throw new RuntimeException(
          String.format(
              "Question is not a multi-option question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public MultiOptionQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
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
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getSelectionPath());
  }

  @Override
  public String getAnswerString() {
    return getSelectedOptionValue().map(LocalizedQuestionOption::optionText).orElse("-");
  }
}
