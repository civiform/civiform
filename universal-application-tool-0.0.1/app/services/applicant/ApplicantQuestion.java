package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import services.Path;
import services.question.AddressQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;
import services.question.TranslationNotFoundException;

/**
 * Represents a question in the context of a specific applicant. Contains non-static inner classes
 * that represent the question as a specific question type (e.g. {@link NameQuestion}). These inner
 * classes provide access to the applicant's answer for the question. They can also implement
 * server-side validation logic.
 */
public class ApplicantQuestion {

  private final QuestionDefinition questionDefinition;
  private final ApplicantData applicantData;

  ApplicantQuestion(QuestionDefinition questionDefinition, ApplicantData applicantData) {
    this.questionDefinition = checkNotNull(questionDefinition);
    this.applicantData = checkNotNull(applicantData);
  }

  public QuestionType getType() {
    return questionDefinition.getQuestionType();
  }

  public String getQuestionText() {
    try {
      return questionDefinition.getQuestionText(applicantData.preferredLocale());
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    return errorsPresenter().getQuestionErrors();
  }

  public boolean hasErrors() {
    if (!getQuestionErrors().isEmpty()) {
      return true;
    }
    return errorsPresenter().hasTypeSpecificErrors();
  }

  public AddressQuestion getAddressQuestion() {
    return new AddressQuestion();
  }

  public TextQuestion getTextQuestion() {
    return new TextQuestion();
  }

  public NameQuestion getNameQuestion() {
    return new NameQuestion();
  }

  private PresentsErrors errorsPresenter() {
    switch (getType()) {
      case ADDRESS:
        return getAddressQuestion();
      case NAME:
        return getNameQuestion();
      case TEXT:
        return getTextQuestion();
      default:
        throw new RuntimeException("Unrecognized question type: " + getType());
    }
  }

  private interface PresentsErrors {
    /** Returns errors if values do not meet conditions defined by admins. */
    ImmutableSet<ValidationErrorMessage> getQuestionErrors();
    /**
     * Returns true if there is any type specific errors. The validation does not consider
     * admin-defined conditions.
     */
    boolean hasTypeSpecificErrors();
  }

  public class AddressQuestion implements PresentsErrors {

    private Optional<String> streetValue;
    private Optional<String> cityValue;
    private Optional<String> stateValue;
    private Optional<String> zipValue;

    public AddressQuestion() {
      assertQuestionType();
    }

    @Override
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
          .addAll(getAddressErrors())
          .addAll(getStreetErrors())
          .addAll(getCityErrors())
          .addAll(getStateErrors())
          .addAll(getZipErrors())
          .build();
    }

    public ImmutableSet<ValidationErrorMessage> getAddressErrors() {
      // TODO: Implement address validation.
      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getStreetErrors() {
      if (hasStreetValue() && getStreetValue().get().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("Street is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getCityErrors() {
      if (hasCityValue() && getCityValue().get().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("City is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getStateErrors() {
      // TODO: Validate state further.
      if (hasStateValue() && getStateValue().get().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("State is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getZipErrors() {
      if (hasZipValue()) {
        String zipValue = getZipValue().get();
        if (zipValue.isEmpty()) {
          return ImmutableSet.of(ValidationErrorMessage.create("Zip code is required."));
        }

        Pattern pattern = Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$");
        Matcher matcher = pattern.matcher(zipValue);
        if (!matcher.matches()) {
          return ImmutableSet.of(ValidationErrorMessage.create("Invalid zip code."));
        }
      }

      return ImmutableSet.of();
    }

    public boolean hasStreetValue() {
      return getStreetValue().isPresent();
    }

    public boolean hasCityValue() {
      return getCityValue().isPresent();
    }

    public boolean hasStateValue() {
      return getStateValue().isPresent();
    }

    public boolean hasZipValue() {
      return getZipValue().isPresent();
    }

    public Optional<String> getStreetValue() {
      if (streetValue != null) {
        return streetValue;
      }

      streetValue = applicantData.readString(getStreetPath());
      return streetValue;
    }

    public Optional<String> getCityValue() {
      if (cityValue != null) {
        return cityValue;
      }

      cityValue = applicantData.readString(getCityPath());
      return cityValue;
    }

    public Optional<String> getStateValue() {
      if (stateValue != null) {
        return stateValue;
      }

      stateValue = applicantData.readString(getStatePath());
      return stateValue;
    }

    public Optional<String> getZipValue() {
      if (zipValue != null) {
        return zipValue;
      }

      zipValue = applicantData.readString(getZipPath());
      return zipValue;
    }

    public void assertQuestionType() {
      if (!getType().equals(QuestionType.ADDRESS)) {
        throw new RuntimeException(
            String.format(
                "Question is not an ADDRESS question: %s (type: %s)",
                questionDefinition.getPath(), questionDefinition.getQuestionType()));
      }
    }

    public AddressQuestionDefinition getQuestionDefinition() {
      assertQuestionType();
      return (AddressQuestionDefinition) questionDefinition;
    }

    public Path getStreetPath() {
      return getQuestionDefinition().getStreetPath();
    }

    public Path getCityPath() {
      return getQuestionDefinition().getCityPath();
    }

    public Path getStatePath() {
      return getQuestionDefinition().getStatePath();
    }

    public Path getZipPath() {
      return getQuestionDefinition().getZipPath();
    }
  }

  public class TextQuestion implements PresentsErrors {

    private Optional<String> textValue;

    public TextQuestion() {
      assertQuestionType();
    }

    @Override
    public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
      TextQuestionDefinition definition = getQuestionDefinition();
      int textLength = getTextValue().map(s -> s.length()).orElse(0);
      ImmutableSet.Builder<ValidationErrorMessage> errors =
          ImmutableSet.<ValidationErrorMessage>builder();

      if (definition.getMinLength().isPresent()) {
        int minLength = definition.getMinLength().getAsInt();
        if (textLength < minLength) {
          errors.add(
              ValidationErrorMessage.create(
                  String.format(
                      "text length %d is smaller than min length %d", textLength, minLength)));
        }
      }

      if (definition.getMaxLength().isPresent()) {
        int maxLength = definition.getMaxLength().getAsInt();
        if (textLength > maxLength) {
          errors.add(
              ValidationErrorMessage.create(
                  String.format(
                      "text length %d is larger than max length %d", textLength, maxLength)));
        }
      }

      return errors.build();
    }

    @Override
    public boolean hasTypeSpecificErrors() {
      // There are no inherent requirements in a text question.
      return false;
    }

    public boolean hasValue() {
      return getTextValue().isPresent();
    }

    public Optional<String> getTextValue() {
      if (textValue != null) {
        return textValue;
      }

      textValue = applicantData.readString(questionDefinition.getPath());

      return textValue;
    }

    public void assertQuestionType() {
      if (!getType().equals(QuestionType.TEXT)) {
        throw new RuntimeException(
            String.format(
                "Question is not a TEXT question: %s (type: %s)",
                questionDefinition.getPath(), questionDefinition.getQuestionType()));
      }
    }

    public TextQuestionDefinition getQuestionDefinition() {
      assertQuestionType();
      return (TextQuestionDefinition) questionDefinition;
    }

    public Path getTextPath() {
      return getQuestionDefinition().getTextPath();
    }
  }

  public class NameQuestion implements PresentsErrors {

    private Optional<String> firstNameValue;
    private Optional<String> middleNameValue;
    private Optional<String> lastNameValue;

    public NameQuestion() {
      assertQuestionType();
    }

    @Override
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
      if (hasFirstNameValue() && getFirstNameValue().get().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("First name is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getLastNameErrors() {
      if (hasLastNameValue() && getLastNameValue().get().isEmpty()) {
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

      firstNameValue = applicantData.readString(getFirstNamePath());

      return firstNameValue;
    }

    public Optional<String> getMiddleNameValue() {
      if (middleNameValue != null) {
        return middleNameValue;
      }

      middleNameValue = applicantData.readString(getMiddleNamePath());

      return middleNameValue;
    }

    public Optional<String> getLastNameValue() {
      if (lastNameValue != null) {
        return lastNameValue;
      }

      lastNameValue = applicantData.readString(getLastNamePath());

      return lastNameValue;
    }

    public void assertQuestionType() {
      if (!getType().equals(QuestionType.NAME)) {
        throw new RuntimeException(
            String.format(
                "Question is not a NAME question: %s (type: %s)",
                questionDefinition.getPath(), questionDefinition.getQuestionType()));
      }
    }

    public NameQuestionDefinition getQuestionDefinition() {
      assertQuestionType();
      return (NameQuestionDefinition) questionDefinition;
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
  }
}
