package services.applicant.question;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;

public class MultiSelectQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<ImmutableList<LocalizedQuestionOption>> selectedOptionsValue;

  public MultiSelectQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    MultiOptionQuestionDefinition definition = getQuestionDefinition();
    int numberOfSelections = getSelectedOptionsValue().map(ImmutableList::size).orElse(0);
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getMultiOptionValidationPredicates().minChoicesRequired().isPresent()) {
      int minChoicesRequired =
          definition.getMultiOptionValidationPredicates().minChoicesRequired().getAsInt();
      if (numberOfSelections < minChoicesRequired) {
        errors.add(ValidationErrorMessage.tooFewSelectionsError(minChoicesRequired));
      }
    }

    if (definition.getMultiOptionValidationPredicates().maxChoicesAllowed().isPresent()) {
      int maxChoicesAllowed =
          definition.getMultiOptionValidationPredicates().maxChoicesAllowed().getAsInt();
      if (numberOfSelections > maxChoicesAllowed) {
        errors.add(ValidationErrorMessage.tooManySelectionsError(maxChoicesAllowed));
      }
    }
    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // The question does not recognize selected options not present in the options set
    return false;
  }

  public boolean hasValue() {
    return getSelectedOptionsValue().isPresent();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getSelectionPath());
  }

  public Optional<ImmutableList<LocalizedQuestionOption>> getSelectedOptionsValue() {
    if (selectedOptionsValue != null) {
      return selectedOptionsValue;
    }

    Optional<ImmutableList<Long>> maybeOptionIds =
        applicantQuestion.getApplicantData().readList(getSelectionPath());

    if (maybeOptionIds.isEmpty()) {
      selectedOptionsValue = Optional.empty();
      return selectedOptionsValue;
    }

    ImmutableList<Long> optionIds = maybeOptionIds.get();

    selectedOptionsValue =
        Optional.of(
            getOptions().stream()
                .filter(option -> optionIds.contains(option.id()))
                .collect(toImmutableList()));

    return selectedOptionsValue;
  }

  public boolean optionIsSelected(LocalizedQuestionOption option) {
    return getSelectedOptionsValue().isPresent()
        && getSelectedOptionsValue().get().contains(option);
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

  /**
   * For multi-select questions, we must append {@code []} to the field name so that the Play
   * framework allows multiple form keys with the same value. For more information, see
   * https://www.playframework.com/documentation/2.8.x/JavaFormHelpers#Handling-repeated-values
   */
  public String getSelectionPathAsArray() {
    return getSelectionPath().toString() + Path.ARRAY_SUFFIX;
  }

  public Path getSelectionPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.SELECTION);
  }

  public ImmutableList<LocalizedQuestionOption> getOptions() {
    return getQuestionDefinition()
        .getOptionsForLocaleOrDefault(applicantQuestion.getApplicantData().preferredLocale());
  }
}
