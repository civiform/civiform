package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionType;

public class NameQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> firstNameValue;
  private Optional<String> middleNameValue;
  private Optional<String> lastNameValue;

  public NameQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // TODO: Implement admin-defined validation.
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.<ValidationErrorMessage>builder()
        .addAll(getFirstNameErrors())
        .addAll(getLastNameErrors())
        .build();
  }

  public ImmutableSet<ValidationErrorMessage> getFirstNameErrors() {
    if (isFirstNameAnswered() && getFirstNameValue().isEmpty()) {
      return ImmutableSet.of(ValidationErrorMessage.create("First name is required."));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getLastNameErrors() {
    if (isLastNameAnswered() && getLastNameValue().isEmpty()) {
      return ImmutableSet.of(ValidationErrorMessage.create("Last name is required."));
    }

    return ImmutableSet.of();
  }

  public Optional<String> getFirstNameValue() {
    if (firstNameValue != null) {
      return firstNameValue;
    }

    firstNameValue = applicantQuestion.getApplicantData().readString(getFirstNamePath());

    return firstNameValue;
  }

  public Optional<String> getMiddleNameValue() {
    if (middleNameValue != null) {
      return middleNameValue;
    }

    middleNameValue = applicantQuestion.getApplicantData().readString(getMiddleNamePath());

    return middleNameValue;
  }

  public Optional<String> getLastNameValue() {
    if (lastNameValue != null) {
      return lastNameValue;
    }

    lastNameValue = applicantQuestion.getApplicantData().readString(getLastNamePath());

    return lastNameValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.NAME)) {
      throw new RuntimeException(
          String.format(
              "Question is not a NAME question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getPath(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public NameQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (NameQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getFirstNamePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.FIRST_NAME);
  }

  public Path getMiddleNamePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.MIDDLE_NAME);
  }

  public Path getLastNamePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.LAST_NAME);
  }

  private boolean isFirstNameAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getFirstNamePath());
  }

  private boolean isMiddleNameAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getMiddleNamePath());
  }

  private boolean isLastNameAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getLastNamePath());
  }

  /**
   * Returns true if any one of the name fields is answered. Returns false if all are not answered.
   */
  @Override
  public boolean isAnswered() {
    return isFirstNameAnswered() || isMiddleNameAnswered() || isLastNameAnswered();
  }

  @Override
  public String getAnswerString() {
    String[] parts = {
      getFirstNameValue().orElse(""), getMiddleNameValue().orElse(""), getLastNameValue().orElse("")
    };

    return Arrays.stream(parts).filter(part -> part.length() > 0).collect(Collectors.joining(" "));
  }
}
