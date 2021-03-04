package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
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

  public AddressQuestion getAddressQuestion() {
    return new AddressQuestion();
  }

  public TextQuestion getTextQuestion() {
    return new TextQuestion();
  }

  public NameQuestion getNameQuestion() {
    return new NameQuestion();
  }

  public class AddressQuestion {
    private Optional<String> streetValue;
    private Optional<String> cityValue;
    private Optional<String> stateValue;
    private Optional<String> zipValue;

    public AddressQuestion() {
      assertQuestionType();
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

    public String getStreetPath() {
      return getQuestionDefinition().getStreetPath();
    }

    public String getCityPath() {
      return getQuestionDefinition().getCityPath();
    }

    public String getStatePath() {
      return getQuestionDefinition().getStatePath();
    }

    public String getZipPath() {
      return getQuestionDefinition().getZipPath();
    }
  }

  public class TextQuestion {
    private Optional<String> textValue;

    public TextQuestion() {
      assertQuestionType();
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

    public String getTextPath() {
      return getQuestionDefinition().getTextPath();
    }
  }

  public class NameQuestion {
    private Optional<String> firstNameValue;
    private Optional<String> middleNameValue;
    private Optional<String> lastNameValue;

    public NameQuestion() {
      assertQuestionType();
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

    public String getMiddleNamePath() {
      return getQuestionDefinition().getMiddleNamePath();
    }

    public String getFirstNamePath() {
      return getQuestionDefinition().getFirstNamePath();
    }

    public String getLastNamePath() {
      return getQuestionDefinition().getLastNamePath();
    }
  }
}
