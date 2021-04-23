package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;

// TODO(https://github.com/seattle-uat/civiform/issues/396): Implement a question that allows for
// multiple answer selections (i.e. the value is a list)
public class SingleSelectQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<LocalizedQuestionOption> selectedOptionValue;

  public SingleSelectQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // Only one selection is possible - there is no admin-configured validation.
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // Does not recognize invalid values
    return false;
  }

  public boolean hasValue() {
    return getSelectedOptionValue().isPresent();
  }

  public Optional<LocalizedQuestionOption> getSelectedOptionValue() {
    if (selectedOptionValue != null) {
      return selectedOptionValue;
    }

    Optional<Long> selectedOptionId =
        applicantQuestion.getApplicantData().readLong(getSelectionPath());

    selectedOptionValue =
        selectedOptionId.isEmpty()
            ? Optional.empty()
            : getOptions().stream()
                .filter(option -> selectedOptionId.get() == option.id())
                .findFirst()
                .or(() -> Optional.empty());

    return selectedOptionValue;
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

  public Path getSelectionPath() {
    return getQuestionDefinition().getSelectionPath();
  }

  public boolean optionIsSelected(LocalizedQuestionOption option) {
    return getSelectedOptionValue().isPresent() && getSelectedOptionValue().get().equals(option);
  }

  public ImmutableList<LocalizedQuestionOption> getOptions() {
    return getQuestionDefinition()
        .getOptionsForLocaleOrDefault(applicantQuestion.getApplicantData().preferredLocale());
  }

  @Override
  public boolean isAnswered() {
    // TODO(https://github.com/seattle-uat/civiform/issues/783): Use hydrated path.
    return applicantQuestion.getApplicantData().hasPath(getSelectionPath());
  }

  @Override
  public String getAnswerString() {
    Optional<LocalizedQuestionOption> option = this.getSelectedOptionValue();
    if (option.isPresent()) {
      return option.get().optionText();
    }
    return "-";
  }
}
