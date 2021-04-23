package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import services.LocalizationUtils;
import services.Path;
import services.question.exceptions.TranslationNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public abstract class QuestionForm {
  private String questionName;
  private String questionDescription;
  private Optional<Long> repeaterId;
  private String questionText;
  private String questionHelpText;

  protected QuestionForm() {
    questionName = "";
    questionDescription = "";
    repeaterId = Optional.empty();
    questionText = "";
    questionHelpText = "";
  }

  protected QuestionForm(QuestionDefinition qd) {
    questionName = qd.getName();
    questionDescription = qd.getDescription();
    repeaterId = qd.getRepeaterId();

    try {
      questionText = qd.getQuestionText(LocalizationUtils.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      questionText = "Missing Text";
    }

    try {
      questionHelpText = qd.getQuestionHelpText(LocalizationUtils.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      questionHelpText = "Missing Text";
    }
  }

  public String getQuestionName() {
    return questionName;
  }

  public void setQuestionName(String questionName) {
    this.questionName = checkNotNull(questionName);
  }

  public String getQuestionDescription() {
    return questionDescription;
  }

  public void setQuestionDescription(String questionDescription) {
    this.questionDescription = checkNotNull(questionDescription);
  }

  public Optional<Long> getRepeaterId() {
    return repeaterId;
  }

  public void setRepeaterId(String repeaterId) {
    this.repeaterId =
        repeaterId.isEmpty() ? Optional.empty() : Optional.of(Long.valueOf(repeaterId));
  }

  public abstract QuestionType getQuestionType();

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = checkNotNull(questionText);
  }

  public String getQuestionHelpText() {
    return questionHelpText;
  }

  public void setQuestionHelpText(String questionHelpText) {
    this.questionHelpText = checkNotNull(questionHelpText);
  }

  public QuestionDefinitionBuilder getBuilder(Path path) {
    ImmutableMap<Locale, String> questionTextMap =
        questionText.isEmpty() ? ImmutableMap.of() : ImmutableMap.of(Locale.US, questionText);
    ImmutableMap<Locale, String> questionHelpTextMap =
        questionHelpText.isEmpty()
            ? ImmutableMap.of()
            : ImmutableMap.of(Locale.US, questionHelpText);

    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(getQuestionType())
            .setName(questionName)
            .setPath(path)
            .setDescription(questionDescription)
            .setRepeaterId(repeaterId)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    return builder;
  }
}
