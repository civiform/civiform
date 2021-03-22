package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;
import services.question.AddressQuestionDefinition.AddressValidationPredicates;
import services.question.NameQuestionDefinition.NameValidationPredicates;
import services.question.QuestionDefinition.ValidationPredicates;
import services.question.TextQuestionDefinition.TextValidationPredicates;

public class QuestionDefinitionBuilder {

  private OptionalLong id = OptionalLong.empty();
  private long version;
  private String name;
  private Path path;
  private String description;
  private ImmutableMap<Locale, String> questionText;
  private ImmutableMap<Locale, String> questionHelpText = ImmutableMap.of();
  private QuestionType questionType = QuestionType.TEXT;
  private String validationPredicatesString = "";

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
    validationPredicatesString = definition.getValidationPredicatesAsString();
  }

  public QuestionDefinitionBuilder clearId() {
    this.id = OptionalLong.empty();
    return this;
  }

  public QuestionDefinitionBuilder setId(long id) {
    this.id = OptionalLong.of(id);
    return this;
  }

  public static QuestionDefinitionBuilder sample() {
    return sample(QuestionType.TEXT);
  }

  public static QuestionDefinitionBuilder sample(QuestionType questionType) {
    return new QuestionDefinitionBuilder()
        .setName("")
        .setDescription("")
        .setPath(Path.create("sample.question.path"))
        .setQuestionText(ImmutableMap.of(Locale.US, "Sample question text"))
        .setQuestionHelpText(ImmutableMap.of(Locale.US, "Sample question help text"))
        .setQuestionType(questionType);
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

  public QuestionDefinitionBuilder setValidationPredicatesString(
      String validationPredicatesString) {
    this.validationPredicatesString = validationPredicatesString;
    return this;
  }

  public QuestionDefinitionBuilder setValidationPredicates(
      ValidationPredicates validationPredicates) {
    this.validationPredicatesString = validationPredicates.serializeAsString();
    return this;
  }

  public QuestionDefinition build() throws UnsupportedQuestionTypeException {
    switch (this.questionType) {
      case ADDRESS:
        AddressValidationPredicates addressValidationPredicates =
            AddressValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          addressValidationPredicates =
              AddressValidationPredicates.parse(validationPredicatesString);
        }
        return new AddressQuestionDefinition(
            id,
            version,
            name,
            path,
            description,
            questionText,
            questionHelpText,
            addressValidationPredicates);
      case NAME:
        NameValidationPredicates nameValidationPredicates = NameValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          nameValidationPredicates = NameValidationPredicates.parse(validationPredicatesString);
        }
        return new NameQuestionDefinition(
            id,
            version,
            name,
            path,
            description,
            questionText,
            questionHelpText,
            nameValidationPredicates);
      case TEXT:
        TextValidationPredicates textValidationPredicates = TextValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          textValidationPredicates = TextValidationPredicates.parse(validationPredicatesString);
        }
        return new TextQuestionDefinition(
            id,
            version,
            name,
            path,
            description,
            questionText,
            questionHelpText,
            textValidationPredicates);
      default:
        throw new UnsupportedQuestionTypeException(this.questionType);
    }
  }
}
