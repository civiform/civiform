package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.Path;
import services.question.exceptions.TranslationNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public abstract class QuestionForm {
  private String questionName;
  private String questionDescription;
  private String questionText;
  private String questionHelpText;

  protected QuestionForm() {
    questionName = "";
    questionDescription = "";
    questionText = "";
    questionHelpText = "";
  }

  protected QuestionForm(QuestionDefinition qd) {
    questionName = qd.getName();
    questionDescription = qd.getDescription();

    try {
      questionText = qd.getQuestionText(Locale.US);
    } catch (TranslationNotFoundException e) {
      questionText = "Missing Text";
    }

    try {
      questionHelpText = qd.getQuestionHelpText(Locale.US);
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
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    return builder;
  }

  /** This is a temporary fix until Path is properly handled. */
  public Path getPath() {
    String questionNameFormattedForPath =
        questionName.replaceAll("\\s", "_").replaceAll("[^a-zA-Z_]", "");
    if (getQuestionType().equals(QuestionType.REPEATER)) {
      questionNameFormattedForPath += Path.ARRAY_SUFFIX;
    }
    return Path.create("applicant").join(questionNameFormattedForPath);
  }
}
