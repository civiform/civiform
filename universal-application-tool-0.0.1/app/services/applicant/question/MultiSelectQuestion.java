package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.exceptions.TranslationNotFoundException;
import services.question.types.MultiOptionQuestionDefinition;

public class MultiSelectQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<ImmutableList<String>> selectedOptionsValue;

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
    // Return true if the selected options are not valid options.
    return getSelectedOptionsValue().isPresent()
        && !getOptions().containsAll(getSelectedOptionsValue().get());
  }

  public boolean hasValue() {
    return getSelectedOptionsValue().isPresent();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getSelectionPath());
  }

  public Optional<ImmutableList<String>> getSelectedOptionsValue() {
    if (selectedOptionsValue != null) {
      return selectedOptionsValue;
    }

    selectedOptionsValue = applicantQuestion.getApplicantData().readList(getSelectionPath());

    return selectedOptionsValue;
  }

  public boolean optionIsSelected(String option) {
    return getSelectedOptionsValue().isPresent()
        && getSelectedOptionsValue().get().contains(option);
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().isMultiOptionType()) {
      throw new RuntimeException(
          String.format(
              "Question is not a multi-option question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getPath(),
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
    return getQuestionDefinition().getSelectionPath();
  }

  public ImmutableList<String> getOptions() {
    try {
      return getQuestionDefinition()
          .getOptionsForLocale(applicantQuestion.getApplicantData().preferredLocale());
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
