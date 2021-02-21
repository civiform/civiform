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
      return questionDefinition.getQuestionText(Locale.ENGLISH);
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

    public boolean hasFirstNameValue() {
      return getFirstNameValue().isPresent();
    }

    public Optional<String> getFirstNameValue() {
      if (firstNameValue != null) {
        return firstNameValue;
      }

      firstNameValue = applicantData.readText(getFirstNamePath());

      return firstNameValue;
    }

    public String getFirstNamePath() {
      return questionDefinition.getPath() + ".first";
    }
  }
}
