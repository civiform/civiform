package forms;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public class QuestionForm {
  private String questionName;
  private String questionDescription;
  private String questionPath;
  private String questionType;
  private String questionText;
  private String questionHelpText;

  public String getQuestionName() {
    return questionName;
  }

  public void setQuestionName(String questionName) {
    this.questionName = questionName;
  }

  public String getQuestionDescription() {
    return questionDescription;
  }

  public void setQuestionDescription(String questionDescription) {
    this.questionDescription = questionDescription;
  }

  public String getQuestionPath() {
    return questionPath;
  }

  public void setQuestionPath(String questionPath) {
    this.questionPath = questionPath;
  }

  public String getQuestionType() {
    return questionType;
  }

  public void setQuestionType(String questionType) {
    this.questionType = questionType;
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public String getQuestionHelpText() {
    return questionHelpText;
  }

  public void setQuestionHelpText(String questionHelpText) {
    this.questionHelpText = questionHelpText;
  }

  public QuestionDefinitionBuilder getBuilder() {
    ImmutableMap<Locale, String> questionTextMap =
        questionText.isEmpty()
            ? ImmutableMap.of()
            : ImmutableMap.of(Locale.ENGLISH, questionText);
    Optional<ImmutableMap<Locale, String>> questionHelpTextMap =
        questionHelpText.isEmpty()
            ? Optional.empty()
            : Optional.of(ImmutableMap.of(Locale.ENGLISH, questionHelpText));
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.valueOf(questionType))
            .setName(questionName == null ? "" : questionName)
            .setPath(questionPath == null ? "" : questionPath)
            .setDescription(questionDescription == null ? "" : questionDescription)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    return builder;
  }
}
