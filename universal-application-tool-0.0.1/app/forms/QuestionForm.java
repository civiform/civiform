package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;
import services.question.InvalidQuestionTypeException;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TranslationNotFoundException;

public class QuestionForm {
  private OptionalLong questionId;
  private long questionVersion;
  private String questionName;
  private String questionDescription;
  private Path questionPath;
  private String questionType;
  private String questionText;
  private String questionHelpText;

  public QuestionForm() {
    questionId = OptionalLong.empty();
    questionVersion = 1L;
    questionName = "";
    questionDescription = "";
    questionPath = Path.empty();
    questionType = "TEXT";
    questionText = "";
    questionHelpText = "";
  }

  public QuestionForm(QuestionDefinition qd) {
    questionId = OptionalLong.of(qd.getId());
    questionVersion = qd.getVersion();
    questionName = qd.getName();
    questionDescription = qd.getDescription();
    questionPath = qd.getPath();
    questionType = qd.getQuestionType().name();

    try {
      questionText = qd.getQuestionText(Locale.ENGLISH);
    } catch (TranslationNotFoundException e) {
      questionText = "Missing Text";
    }

    try {
      questionHelpText = qd.getQuestionHelpText(Locale.ENGLISH);
    } catch (TranslationNotFoundException e) {
      questionHelpText = "Missing Text";
    }
  }

  public boolean hasQuestionId() {
    return questionId.isPresent();
  }

  public long getQuestionId() {
    return questionId.getAsLong();
  }

  public String getQuestionIdString() {
    if (questionId.isPresent()) {
      return Long.toString(questionId.getAsLong());
    }
    return "";
  }

  public void setQuestionId(String questionId) {
    if (!questionId.isBlank()) {
      this.questionId = OptionalLong.of(Long.parseLong(questionId));
    }
  }

  public long getQuestionVersion() {
    return questionVersion;
  }

  public String getQuestionVersionString() {
    return Long.toString(questionVersion);
  }

  public void setQuestionVersion(String questionVersion) {
    this.questionVersion = Long.parseLong(questionVersion);
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

  public Path getQuestionPath() {
    return questionPath;
  }

  public void setQuestionPath(String questionPath) {
    this.questionPath = Path.create(checkNotNull(questionPath));
  }

  public String getQuestionType() {
    return questionType;
  }

  public void setQuestionType(String questionType) {
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

  public QuestionDefinitionBuilder getBuilder() throws InvalidQuestionTypeException {
    ImmutableMap<Locale, String> questionTextMap =
        questionText.isEmpty() ? ImmutableMap.of() : ImmutableMap.of(Locale.ENGLISH, questionText);
    ImmutableMap<Locale, String> questionHelpTextMap =
        questionHelpText.isEmpty()
            ? ImmutableMap.of()
            : ImmutableMap.of(Locale.ENGLISH, questionHelpText);
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.of(questionType))
            .setVersion(questionVersion)
            .setName(questionName)
            .setPath(questionPath)
            .setDescription(questionDescription)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    if (questionId.isPresent()) {
      builder.setId(questionId.getAsLong());
    }
    return builder;
  }
}
