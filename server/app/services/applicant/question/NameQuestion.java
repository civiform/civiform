package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a name question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class NameQuestion extends QuestionImpl {

  private Optional<String> firstNameValue;
  private Optional<String> middleNameValue;
  private Optional<String> lastNameValue;

  public NameQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.NAME);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getFirstNamePath(), getMiddleNamePath(), getLastNamePath());
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // TODO: Implement admin-defined validation.
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.<ValidationErrorMessage>builder()
        .addAll(getFirstNameErrors())
        .addAll(getLastNameErrors())
        .build();
  }

  public ImmutableSet<ValidationErrorMessage> getFirstNameErrors() {
    if (isAnswered() && getFirstNameValue().isEmpty()) {
      return getFirstNameErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getFirstNameErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.NAME_VALIDATION_FIRST_REQUIRED));
  }

  public ImmutableSet<ValidationErrorMessage> getLastNameErrors() {
    if (isAnswered() && getLastNameValue().isEmpty()) {
      return getLastNameErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getLastNameErrorMessage() {
    return ImmutableSet.of(ValidationErrorMessage.create(MessageKey.NAME_VALIDATION_LAST_REQUIRED));
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

  public NameQuestionDefinition getQuestionDefinition() {
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

  @Override
  public String getAnswerString() {
    String[] parts = {
      getFirstNameValue().orElse(""), getMiddleNameValue().orElse(""), getLastNameValue().orElse("")
    };

    return Arrays.stream(parts).filter(part -> part.length() > 0).collect(Collectors.joining(" "));
  }
}
