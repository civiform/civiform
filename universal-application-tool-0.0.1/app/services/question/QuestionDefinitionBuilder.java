package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class QuestionDefinitionBuilder {

  private OptionalLong id = OptionalLong.empty();
  private long version;
  private String name;
  private Path path;
  private String description;
  private ImmutableMap<Locale, String> questionText;
  private ImmutableMap<Locale, String> questionHelpText = ImmutableMap.of();
  private QuestionType questionType = QuestionType.TEXT;
  private ImmutableMap<ValidationPredicate, String> validationPredicates = ImmutableMap.of();

  public QuestionDefinitionBuilder() {}

  public QuestionDefinitionBuilder(QuestionDefinition definition) {
    if (definition.isPersisted()) {
      long definitionId = definition.getId();
      this.id = OptionalLong.of(definitionId);
    }
    version = definition.getVersion();
    name = definition.getName();
    path = definition.getPath();
    description = definition.getDescription();
    questionText = definition.getQuestionText();
    questionHelpText = definition.getQuestionHelpText();
    questionType = definition.getQuestionType();
    validationPredicates = definition.getValidationPredicates();
  }

  public QuestionDefinitionBuilder clearId() {
    this.id = OptionalLong.empty();
    return this;
  }

  public QuestionDefinitionBuilder setId(long id) {
    this.id = OptionalLong.of(id);
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

  public QuestionDefinitionBuilder setPath(Path path) {
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
      ImmutableMap<Locale, String> questionHelpText) {
    this.questionHelpText = questionHelpText;
    return this;
  }

  public QuestionDefinitionBuilder setQuestionType(QuestionType questionType) {
    this.questionType = questionType;
    return this;
  }

  public QuestionDefinitionBuilder setValidationPredicates(
      ImmutableMap<ValidationPredicate, String> validationPredicates) {
    this.validationPredicates = validationPredicates;
    return this;
  }

  public QuestionDefinition build() throws UnsupportedQuestionTypeException {
    switch (this.questionType) {
      case ADDRESS:
        return new AddressQuestionDefinition(
            id,
            version,
            name,
            path,
            description,
            questionText,
            questionHelpText,
            validationPredicates);
      case NAME:
        return new NameQuestionDefinition(
            id,
            version,
            name,
            path,
            description,
            questionText,
            questionHelpText,
            validationPredicates);
      case TEXT:
        return new TextQuestionDefinition(
            id,
            version,
            name,
            path,
            description,
            questionText,
            questionHelpText,
            validationPredicates);
      default:
        throw new UnsupportedQuestionTypeException(this.questionType);
    }
  }
}
