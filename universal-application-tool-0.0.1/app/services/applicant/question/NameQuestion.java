package services.applicant.question;

import java.util.Optional;

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
    if (firstNameAnswered() && getFirstNameValue().isEmpty()) {
      return ImmutableSet.of(ValidationErrorMessage.create("First name is required."));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getLastNameErrors() {
    if (lastNameAnswered() && getLastNameValue().isEmpty()) {
      return ImmutableSet.of(ValidationErrorMessage.create("Last name is required."));
    }

    return ImmutableSet.of();
  }

  public boolean hasFirstNameValue() {
    return getFirstNameValue().isPresent();
  }

  public boolean hasMiddleNameValue() {
    return getMiddleNameValue().isPresent();
  }

  public boolean hasLastNameValue() {
    return getLastNameValue().isPresent();
  }

  public Optional<String> getFirstNameValue() {
    if (firstNameValue != null) {
      return firstNameValue;
    }

    firstNameValue = applicantQuestion.applicantData.readString(getFirstNamePath());

    return firstNameValue;
  }

  public Optional<String> getMiddleNameValue() {
    if (middleNameValue != null) {
      return middleNameValue;
    }

    middleNameValue = applicantQuestion.applicantData.readString(getMiddleNamePath());

    return middleNameValue;
  }

  public Optional<String> getLastNameValue() {
    if (lastNameValue != null) {
      return lastNameValue;
    }

    lastNameValue = applicantQuestion.applicantData.readString(getLastNamePath());

    return lastNameValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.NAME)) {
      throw new RuntimeException(
              String.format(
                      "Question is not a NAME question: %s (type: %s)",
                      applicantQuestion.questionDefinition.getPath(), applicantQuestion.questionDefinition.getQuestionType()));
    }
  }

  public NameQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (NameQuestionDefinition) applicantQuestion.questionDefinition;
  }

  public Path getMiddleNamePath() {
    return getQuestionDefinition().getMiddleNamePath();
  }

  public Path getFirstNamePath() {
    return getQuestionDefinition().getFirstNamePath();
  }

  public Path getLastNamePath() {
    return getQuestionDefinition().getLastNamePath();
  }

  private boolean firstNameAnswered() {
    return applicantQuestion.applicantData.hasPath(getFirstNamePath());
  }

  private boolean lastNameAnswered() {
    return applicantQuestion.applicantData.hasPath(getLastNamePath());
  }
}
