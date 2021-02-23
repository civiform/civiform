package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.Optional;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import services.question.TranslationNotFoundException;

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

  public TextQuestion getTextQuestion() {
    if (!getType().equals(QuestionType.TEXT)) {
      throw new RuntimeException("Question is not a TEXT question: " + questionDefinition.getPath());
    }

    return new TextQuestion();
  }

  public NameQuestion getNameQuestion() {
    if (!getType().equals(QuestionType.NAME)) {
      throw new RuntimeException("Question is not a NAME question: " + questionDefinition.getPath());
    }

    return new NameQuestion();
  }

  public class TextQuestion {
    private Optional<String> textValue;

    public boolean hasValue() {
      return getTextValue().isPresent();
    }

    public Optional<String> getTextValue() {
      if (textValue != null) {
        return textValue;
      }

      textValue = applicantData.readText(questionDefinition.getPath());

      return textValue;
    }

    public String getTextPath() {
      return questionDefinition.getPath();
    }
  }

  public class NameQuestion {
    private Optional<String> firstNameValue;
    private Optional<String> middleNameValue;
    private Optional<String> lastNameValue;

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

      firstNameValue = applicantData.readText(getFirstNamePath());

      return firstNameValue;
    }

    public Optional<String> getMiddleNameValue() {
      if (middleNameValue != null) {
        return middleNameValue;
      }

      middleNameValue = applicantData.readText(getMiddleNamePath());

      return middleNameValue;
    }

    public Optional<String> getLastNameValue() {
      if (lastNameValue != null) {
        return lastNameValue;
      }

      lastNameValue = applicantData.readText(getLastNamePath());

      return lastNameValue;
    }

    public String getMiddleNamePath() {
      return questionDefinition.getPath() + ".middle";
    }

    public String getFirstNamePath() {
      return questionDefinition.getPath() + ".first";
    }

    public String getLastNamePath() {
      return questionDefinition.getPath() + ".last";
    }
  }
}
