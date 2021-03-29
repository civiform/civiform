package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.Path;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TranslationNotFoundException;

public class QuestionForm {
  private String questionName;
  private String questionDescription;
  private Path questionParentPath;
  private QuestionType questionType;
  private String questionText;
  private String questionHelpText;

  public QuestionForm() {
    questionName = "";
    questionDescription = "";
    questionParentPath = Path.empty();
    // TODO(natsid): This should be initialized in subclass constructor.
    questionType = QuestionType.TEXT;
    questionText = "";
    questionHelpText = "";
  }

  public QuestionForm(QuestionDefinition qd) {
    questionName = qd.getName();
    questionDescription = qd.getDescription();
    questionParentPath = qd.getPath().parentPath();
    questionType = qd.getQuestionType();

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

  public Path getQuestionParentPath() {
    return questionParentPath;
  }

  public void setQuestionParentPath(String questionParentPath) {
    this.questionParentPath = Path.create(checkNotNull(questionParentPath));
  }

  public Path getQuestionPath() {
    String questionNameFormattedForPath =
        questionName.replaceAll("\\s", "_").replaceAll("[^a-zA-Z_]", "");
    return questionParentPath.join(questionNameFormattedForPath);
  }

  public QuestionType getQuestionType() {
    return questionType;
  }

  // TODO(natsid): Make this protected and only set in the subclasses.
  public void setQuestionType(QuestionType questionType) {
    this.questionType = checkNotNull(questionType);
  }

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

  public QuestionDefinitionBuilder getBuilder() {
    ImmutableMap<Locale, String> questionTextMap =
        questionText.isEmpty() ? ImmutableMap.of() : ImmutableMap.of(Locale.US, questionText);
    ImmutableMap<Locale, String> questionHelpTextMap =
        questionHelpText.isEmpty()
            ? ImmutableMap.of()
            : ImmutableMap.of(Locale.US, questionHelpText);

    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(questionType)
            .setName(questionName)
            .setPath(getQuestionPath())
            .setDescription(questionDescription)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    return builder;
  }
}
