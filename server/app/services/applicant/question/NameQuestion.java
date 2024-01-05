package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.PrimaryApplicantInfoTag;
import services.question.types.NameQuestionDefinition;

/**
 * Represents a name question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class NameQuestion extends Question {

  private Optional<String> firstNameValue;
  private Optional<String> middleNameValue;
  private Optional<String> lastNameValue;

  NameQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getFirstNamePath(), getMiddleNamePath(), getLastNamePath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // TODO: Implement admin-defined validation.
    return ImmutableMap.of(
        getFirstNamePath(), validateFirstName(),
        getLastNamePath(), validateLastName());
  }

  private ImmutableSet<ValidationErrorMessage> validateFirstName() {
    if (getFirstNameValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.NAME_VALIDATION_FIRST_REQUIRED));
    }

    return ImmutableSet.of();
  }

  private ImmutableSet<ValidationErrorMessage> validateLastName() {
    if (getLastNameValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.NAME_VALIDATION_LAST_REQUIRED));
    }

    return ImmutableSet.of();
  }

  private boolean containsNameTag() {
    return applicantQuestion.getQuestionDefinition().containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_NAME);
  }

  public Optional<String> getFirstNameValue() {
    if (firstNameValue != null) {
      return firstNameValue;
    }

    Optional<String> maybeFirstName = applicantQuestion.getApplicantData().getApplicant().getFirstName();
    if (containsNameTag() && maybeFirstName.isPresent()) {
      firstNameValue = maybeFirstName;
    } else {
      firstNameValue = applicantQuestion.getApplicantData().readString(getFirstNamePath());
    }

    return firstNameValue;
  }

  public Optional<String> getMiddleNameValue() {
    if (middleNameValue != null) {
      return middleNameValue;
    }

    Optional<String> maybeMiddleName = applicantQuestion.getApplicantData().getApplicant().getMiddleName();
    if (containsNameTag() && maybeMiddleName.isPresent()) {
      middleNameValue = maybeMiddleName;
    } else {
      middleNameValue = applicantQuestion.getApplicantData().readString(getMiddleNamePath());
    }

    return middleNameValue;
  }

  public Optional<String> getLastNameValue() {
    if (lastNameValue != null) {
      return lastNameValue;
    }

    Optional<String> maybeLastName = applicantQuestion.getApplicantData().getApplicant().getLastName();
    if (containsNameTag() && maybeLastName.isPresent()) {
      lastNameValue = maybeLastName;
    } else {
      lastNameValue = applicantQuestion.getApplicantData().readString(getLastNamePath());
    }
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
