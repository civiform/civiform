package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;

public class QuestionDefinitionBuilder {

  private long id;
  private long version;
  private String name;
  private String path;
  private String description;
  private ImmutableMap<Locale, String> questionText;
  private Optional<ImmutableMap<Locale, String>> questionHelpText;
  private QuestionType questionType = QuestionType.TEXT;

  public QuestionDefinitionBuilder setId(long id) {
    this.id = id;
    return this;
  }

  public QuestionDefinitionBuilder setVersion(long version) {
    this.version = version;
    return this;
  }

  public QuestionDefinitionBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public QuestionDefinitionBuilder setPath(String path) {
    this.path = path;
    return this;
  }

  public QuestionDefinitionBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public QuestionDefinitionBuilder setQuestionText(ImmutableMap<Locale, String> questionText) {
    this.questionText = questionText;
    return this;
  }

  public QuestionDefinitionBuilder setQuestionHelpText(
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    this.questionHelpText = questionHelpText;
    return this;
  }

  public QuestionDefinitionBuilder setQuestionType(QuestionType questionType) {
    this.questionType = questionType;
    return this;
  }

  public QuestionDefinition build() {
    switch (this.questionType) {
      case ADDRESS:
        return new AddressQuestionDefinition(
            id, version, name, path, description, questionText, questionHelpText);
      case NAME:
        return new NameQuestionDefinition(
            id, version, name, path, description, questionText, questionHelpText);
      case TEXT: // fallthrough intended.
      default:
        return new QuestionDefinition(
            id, version, name, path, description, questionText, questionHelpText);
    }
  }
}
