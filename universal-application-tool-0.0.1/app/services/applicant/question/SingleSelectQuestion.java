package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import play.i18n.Messages;
import services.Path;
import services.question.LocalizedQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;

public class SingleSelectQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<LocalizedQuestionOption> selectedOptionValue;

  public SingleSelectQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors(Messages messages) {
    return !getQuestionErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getQuestionErrors(Messages messages) {
    // Only one selection is possible - there is no admin-configured validation.
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors(Messages messages) {
    return !getAllTypeSpecificErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getAllTypeSpecificErrors(Messages messages) {
    // Only one selection is possible - there is no admin-configured validation.
    return ImmutableSet.of();
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

  public ImmutableList<LocalizedQuestionOption> getOptions() {
    return getQuestionDefinition()
        .getOptionsForLocaleOrDefault(applicantQuestion.getApplicantData().preferredLocale());
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getSelectionPath());
  }
}
