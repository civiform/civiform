package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.MultiOptionQuestionDefinition;
import services.question.TranslationNotFoundException;

import java.util.Optional;

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
    // TODO(https://github.com/seattle-uat/civiform/issues/416): Implement validation
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // There are no inherent requirements in a multi-option question.
    return false;
  }

  public boolean hasValue() {
    return getSelectedOptionsValue().isPresent();
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
                      applicantQuestion.getQuestionDefinition().getPath(), applicantQuestion.getQuestionDefinition().getQuestionType()));
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
      return getQuestionDefinition().getOptionsForLocale(applicantQuestion.getApplicantData().preferredLocale());
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
