package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import services.Path;
import services.question.AddressQuestionDefinition;
import services.question.MultiOptionQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.NumberQuestionDefinition;
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

  public ApplicantQuestion(QuestionDefinition questionDefinition, ApplicantData applicantData) {
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

  public String getQuestionHelpText() {
    try {
      return questionDefinition.getQuestionHelpText(applicantData.preferredLocale());
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Path getPath() {
    return questionDefinition.getPath();
  }

  public boolean hasQuestionErrors() {
    return errorsPresenter().hasQuestionErrors();
  }

  public boolean hasErrors() {
    if (hasQuestionErrors()) {
      return true;
    }
    return errorsPresenter().hasTypeSpecificErrors();
  }

  public Optional<Long> getUpdatedInProgramMetadata() {
    return applicantData.readLong(questionDefinition.getProgramIdPath());
  }

  public Optional<Long> getLastUpdatedTimeMetadata() {
    return applicantData.readLong(questionDefinition.getLastUpdatedTimePath());
  }

  public AddressQuestion getAddressQuestion() {
    return new AddressQuestion();
  }

  public SingleSelectQuestion getSingleSelectQuestion() {
    return new SingleSelectQuestion();
  }

  public TextQuestion getTextQuestion() {
    return new TextQuestion();
  }

  public NameQuestion getNameQuestion() {
    return new NameQuestion();
  }

  public NumberQuestion getNumberQuestion() {
    return new NumberQuestion();
  }

  public PresentsErrors errorsPresenter() {
    switch (getType()) {
      case ADDRESS:
        return getAddressQuestion();
      case DROPDOWN:
        return getSingleSelectQuestion();
      case NAME:
        return getNameQuestion();
      case NUMBER:
        return getNumberQuestion();
      case TEXT:
        return getTextQuestion();
      default:
        throw new RuntimeException("Unrecognized question type: " + getType());
    }
  }

  public interface PresentsErrors {
    /** Returns true if values do not meet conditions defined by admins. */
    boolean hasQuestionErrors();
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
      if (streetAnswered() && getStreetValue().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("Street is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getCityErrors() {
      if (cityAnswered() && getCityValue().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("City is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getStateErrors() {
      // TODO: Validate state further.
      if (stateAnswered() && getStateValue().isEmpty()) {
        return ImmutableSet.of(ValidationErrorMessage.create("State is required."));
      }

      return ImmutableSet.of();
    }

    public ImmutableSet<ValidationErrorMessage> getZipErrors() {
      if (zipAnswered()) {
        Optional<String> zipValue = getZipValue();
        if (zipValue.isEmpty()) {
          return ImmutableSet.of(ValidationErrorMessage.create("Zip code is required."));
        }

        Pattern pattern = Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$");
        Matcher matcher = pattern.matcher(zipValue.get());
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

    private boolean streetAnswered() {
      return applicantData.hasPath(getStreetPath());
    }

    private boolean cityAnswered() {
      return applicantData.hasPath(getCityPath());
    }

    private boolean stateAnswered() {
      return applicantData.hasPath(getStatePath());
    }

    private boolean zipAnswered() {
      return applicantData.hasPath(getZipPath());
    }
  }

  public class TextQuestion implements PresentsErrors {

    private Optional<String> textValue;

    public TextQuestion() {
      assertQuestionType();
    }

    @Override
    public boolean hasQuestionErrors() {
      return !getQuestionErrors().isEmpty();
    }

    public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
      if (!hasValue()) {
        return ImmutableSet.of();
      }

      TextQuestionDefinition definition = getQuestionDefinition();
      int textLength = getTextValue().map(s -> s.length()).orElse(0);
      ImmutableSet.Builder<ValidationErrorMessage> errors =
          ImmutableSet.<ValidationErrorMessage>builder();

      if (definition.getMinLength().isPresent()) {
        int minLength = definition.getMinLength().getAsInt();
        if (textLength < minLength) {
          errors.add(ValidationErrorMessage.textTooShortError(minLength));
        }
      }

      if (definition.getMaxLength().isPresent()) {
        int maxLength = definition.getMaxLength().getAsInt();
        if (textLength > maxLength) {
          errors.add(ValidationErrorMessage.textTooLongError(maxLength));
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

      textValue = applicantData.readString(getTextPath());

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

    private boolean firstNameAnswered() {
      return applicantData.hasPath(getFirstNamePath());
    }

    private boolean lastNameAnswered() {
      return applicantData.hasPath(getLastNamePath());
    }
  }

  public class NumberQuestion implements PresentsErrors {

    private Optional<Long> numberValue;

    public NumberQuestion() {
      assertQuestionType();
    }

    @Override
    public boolean hasQuestionErrors() {
      return !getQuestionErrors().isEmpty();
    }

    public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
      if (!hasValue()) {
        return ImmutableSet.of();
      }

      NumberQuestionDefinition definition = getQuestionDefinition();
      long answer = getNumberValue().get();
      ImmutableSet.Builder<ValidationErrorMessage> errors =
          ImmutableSet.<ValidationErrorMessage>builder();

      if (definition.getMin().isPresent()) {
        long min = definition.getMin().getAsLong();
        if (answer < min) {
          errors.add(ValidationErrorMessage.numberTooSmallError(min));
        }
      }

      if (definition.getMax().isPresent()) {
        long max = definition.getMax().getAsLong();
        if (answer > max) {
          errors.add(ValidationErrorMessage.numberTooLargeError(max));
        }
      }

      return errors.build();
    }

    @Override
    public boolean hasTypeSpecificErrors() {
      // There are no inherent requirements in a number question.
      return false;
    }

    public boolean hasValue() {
      return getNumberValue().isPresent();
    }

    public Optional<Long> getNumberValue() {
      if (numberValue != null) {
        return numberValue;
      }

      numberValue = applicantData.readLong(getNumberPath());

      return numberValue;
    }

    public void assertQuestionType() {
      if (!getType().equals(QuestionType.NUMBER)) {
        throw new RuntimeException(
            String.format(
                "Question is not a NUMBER question: %s (type: %s)",
                questionDefinition.getPath(), questionDefinition.getQuestionType()));
      }
    }

    public NumberQuestionDefinition getQuestionDefinition() {
      assertQuestionType();
      return (NumberQuestionDefinition) questionDefinition;
    }

    public Path getNumberPath() {
      return getQuestionDefinition().getNumberPath();
    }
  }

  // TODO(https://github.com/seattle-uat/civiform/issues/396): Implement a question that allows for
  // multiple answer selections (i.e. the value is a list)
  public class SingleSelectQuestion implements PresentsErrors {

    private Optional<String> selectedOptionValue;

    public SingleSelectQuestion() {
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
      return getSelectedOptionValue().isPresent();
    }

    public Optional<String> getSelectedOptionValue() {
      if (selectedOptionValue != null) {
        return selectedOptionValue;
      }

      selectedOptionValue = applicantData.readString(getSelectionPath());

      return selectedOptionValue;
    }

    public void assertQuestionType() {
      if (!getType().isMultiOptionType()) {
        throw new RuntimeException(
            String.format(
                "Question is not a multi-option question: %s (type: %s)",
                questionDefinition.getPath(), questionDefinition.getQuestionType()));
      }
    }

    public MultiOptionQuestionDefinition getQuestionDefinition() {
      assertQuestionType();
      return (MultiOptionQuestionDefinition) questionDefinition;
    }

    public Path getSelectionPath() {
      return getQuestionDefinition().getSelectionPath();
    }

    public ImmutableList<String> getOptions() {
      try {
        return getQuestionDefinition().getOptionsForLocale(applicantData.preferredLocale());
      } catch (TranslationNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof ApplicantQuestion) {
      ApplicantQuestion that = (ApplicantQuestion) object;
      return this.questionDefinition.equals(that.questionDefinition)
          && this.applicantData.equals(that.applicantData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionDefinition, applicantData);
  }
}
