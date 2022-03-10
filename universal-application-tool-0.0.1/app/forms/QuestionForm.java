package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.Question;
import models.QuestionTag;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.export.ExporterService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Superclass for all forms updating a question. */
public abstract class QuestionForm {
  private String questionName;
  private String questionDescription;
  private Optional<Long> enumeratorId;
  private String questionText;
  private String questionHelpText;
  private Optional<String> questionExportState;
  private QuestionDefinition qd;

  protected QuestionForm() {
    questionName = "";
    questionDescription = "";
    enumeratorId = Optional.empty();
    questionText = "";
    questionHelpText = "";
    questionExportState = Optional.of("");
  }

  protected QuestionForm(QuestionDefinition qd) {
    this.qd = qd;
    questionExportState = Optional.empty();
    questionName = qd.getName();
    questionDescription = qd.getDescription();
    enumeratorId = qd.getEnumeratorId();

    try {
      questionText = qd.getQuestionText().get(LocalizedStrings.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      questionText = "Missing Text";
    }

    try {
      questionHelpText = qd.getQuestionHelpText().get(LocalizedStrings.DEFAULT_LOCALE);
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

  public Optional<Long> getEnumeratorId() {
    return enumeratorId;
  }

  public void setEnumeratorId(String enumeratorId) {
    this.enumeratorId =
        enumeratorId.isEmpty() ? Optional.empty() : Optional.of(Long.valueOf(enumeratorId));
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

  public QuestionDefinitionBuilder getBuilder() {
    LocalizedStrings questionTextMap =
        questionText.isEmpty()
            ? LocalizedStrings.of()
            : LocalizedStrings.of(Locale.US, questionText);
    LocalizedStrings questionHelpTextMap =
        questionHelpText.isEmpty()
            ? LocalizedStrings.empty()
            : LocalizedStrings.of(Locale.US, questionHelpText);

    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(getQuestionType())
            .setName(questionName)
            .setDescription(questionDescription)
            .setEnumeratorId(enumeratorId)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap);
    return builder;
  }

  public void setQuestionExportState(String questionExportState) {
    this.questionExportState = Optional.of(questionExportState);
  }

  public void setQuestionExportStateFromTags(List<QuestionTag> questionTags) {
    if (questionTags.contains(QuestionTag.DEMOGRAPHIC)) {
      questionExportState = Optional.of(QuestionTag.DEMOGRAPHIC.getValue());
    } else if (questionTags.contains(QuestionTag.DEMOGRAPHIC_PII)) {
      questionExportState = Optional.of(QuestionTag.DEMOGRAPHIC_PII.getValue());
    } else {
      questionExportState = Optional.of(QuestionTag.NON_DEMOGRAPHIC.getValue());
    }
  }

  public String getQuestionExportState() {
    if (ExporterService.NON_EXPORTED_QUESTION_TYPES.contains(getQuestionType())) {
      return QuestionTag.NON_DEMOGRAPHIC.getValue();
    }

    if (questionExportState.isEmpty()) {
      Question q = new Question(this.qd);
      q.refresh();
      setQuestionExportStateFromTags(q.getQuestionTags());
    }
    return questionExportState.get();
  }
}
