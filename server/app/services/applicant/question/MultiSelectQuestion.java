package services.applicant.question;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;

/**
 * Represents a multi-select question in the context of a specific applicant.
 *
 * <p>All multi-select question types are meant to share this class, although there is only checkbox
 * question type in this category as of June 30, 2021.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class MultiSelectQuestion extends Question {

  private Optional<ImmutableList<LocalizedQuestionOption>> selectedOptionsValue;

  MultiSelectQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of(applicantQuestion.getContextualizedPath(), validateOptions());
  }

  private ImmutableSet<ValidationErrorMessage> validateOptions() {
    MultiOptionQuestionDefinition definition = getQuestionDefinition();
    int numberOfSelections = getSelectedOptionValues().map(ImmutableList::size).orElse(0);
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getMultiOptionValidationPredicates().minChoicesRequired().isPresent()) {
      int minChoicesRequired =
          definition.getMultiOptionValidationPredicates().minChoicesRequired().getAsInt();
      if (numberOfSelections < minChoicesRequired) {
        errors.add(
            ValidationErrorMessage.create(
                MessageKey.MULTI_SELECT_VALIDATION_TOO_FEW, minChoicesRequired));
      }
    }

    if (definition.getMultiOptionValidationPredicates().maxChoicesAllowed().isPresent()) {
      int maxChoicesAllowed =
          definition.getMultiOptionValidationPredicates().maxChoicesAllowed().getAsInt();
      if (numberOfSelections > maxChoicesAllowed) {
        errors.add(
            ValidationErrorMessage.create(
                MessageKey.MULTI_SELECT_VALIDATION_TOO_MANY, maxChoicesAllowed + 1));
      }
    }
    return errors.build();
  }

  public boolean hasValue() {
    return getSelectedOptionValues().isPresent();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getSelectionPath());
  }

  /** Get the selected options in the applicant's preferred locale. */
  public Optional<ImmutableList<LocalizedQuestionOption>> getSelectedOptionValues() {
    if (selectedOptionsValue == null) {
      selectedOptionsValue =
          getSelectedOptionValues(applicantQuestion.getApplicantData().preferredLocale());
    }
    return selectedOptionsValue;
  }

  /** Get the selected options in the specified locale. */
  public Optional<ImmutableList<LocalizedQuestionOption>> getSelectedOptionValues(Locale locale) {
    Optional<ImmutableList<Long>> maybeOptionIds =
        applicantQuestion.getApplicantData().readLongList(getSelectionPath());

    if (maybeOptionIds.isEmpty()) {
      selectedOptionsValue = Optional.empty();
      return selectedOptionsValue;
    }

    ImmutableList<Long> optionIds = maybeOptionIds.get();

    return Optional.of(
        getOptions(locale).stream()
            .filter(option -> optionIds.contains(option.id()))
            .collect(toImmutableList()));
  }

  public Optional<ImmutableList<String>> getSelectedOptionAdminNames() {
    Optional<ImmutableList<Long>> maybeSelectedOptionIds =
        applicantQuestion.getApplicantData().readLongList(getSelectionPath());

    if (maybeSelectedOptionIds.isEmpty()) {
      return Optional.empty();
    }

    ImmutableList<Long> selectedOptionIds = maybeSelectedOptionIds.get();
    return Optional.of(
        getQuestionDefinition().getOptions().stream()
            .filter(option -> selectedOptionIds.contains(option.id()))
            .map(QuestionOption::adminName)
            .collect(toImmutableList()));
  }

  public boolean optionIsSelected(LocalizedQuestionOption option) {
    return getSelectedOptionValues().isPresent()
        && getSelectedOptionValues().get().contains(option);
  }

  public MultiOptionQuestionDefinition getQuestionDefinition() {
    return (MultiOptionQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  /**
   * For multi-select questions, we must append {@code []} to the field name so that the Play
   * framework allows multiple form keys with the same value. For more information, see
   * https://www.playframework.com/documentation/2.8.x/JavaFormHelpers#Handling-repeated-values
   */
  public String getSelectionPathAsArray() {
    return getSelectionPath().toString() + Path.ARRAY_SUFFIX;
  }

  public Path getSelectionPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.SELECTIONS);
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
    return getSelectedOptionValues()
        .map(
            options ->
                options.stream()
                    .map(LocalizedQuestionOption::optionText)
                    .collect(Collectors.joining("\n")))
        .orElse("-");
  }
}
