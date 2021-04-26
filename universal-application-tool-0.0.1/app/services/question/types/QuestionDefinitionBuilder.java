package services.question.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizationUtils;
import services.Path;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition.AddressValidationPredicates;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.NameQuestionDefinition.NameValidationPredicates;
import services.question.types.QuestionDefinition.ValidationPredicates;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

public class QuestionDefinitionBuilder {

  private OptionalLong id = OptionalLong.empty();
  private String name;
  private Path path;
  private Optional<Long> repeaterId = Optional.empty();
  private String description;
  private ImmutableMap<Locale, String> questionText;
  private ImmutableMap<Locale, String> questionHelpText = ImmutableMap.of();
  private QuestionType questionType = QuestionType.TEXT;
  private String validationPredicatesString = "";

  // Multi-option question types only.
  private ImmutableList<QuestionOption> questionOptions = ImmutableList.of();

  public QuestionDefinitionBuilder() {}

  public QuestionDefinitionBuilder(QuestionDefinition definition) {
    if (definition.isPersisted()) {
      long definitionId = definition.getId();
      this.id = OptionalLong.of(definitionId);
    }
    name = definition.getName();
    path = definition.getPath();
    repeaterId = definition.getRepeaterId();
    description = definition.getDescription();
    questionText = definition.getQuestionText();
    questionHelpText = definition.getQuestionHelpText();
    questionType = definition.getQuestionType();
    validationPredicatesString = definition.getValidationPredicatesAsString();

    if (definition.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;
      questionOptions = multiOption.getOptions();
    }
  }

  public static QuestionDefinitionBuilder sample() {
    return sample(QuestionType.TEXT);
  }

  public static QuestionDefinitionBuilder sample(QuestionType questionType) {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setName("")
            .setDescription("")
            .setPath(Path.create("sample.question.path"))
            .setQuestionText(ImmutableMap.of(Locale.US, "Sample question text"))
            .setQuestionHelpText(ImmutableMap.of(Locale.US, "Sample question help text"))
            .setQuestionType(questionType);

    if (questionType.isMultiOptionType()) {
      builder.setQuestionOptions(
          ImmutableList.of(
              QuestionOption.create(1L, ImmutableMap.of(Locale.US, "Sample question option"))));
    }

    return builder;
  }

  public QuestionDefinitionBuilder clearId() {
    this.id = OptionalLong.empty();
    return this;
  }

  public QuestionDefinitionBuilder setId(Void v) {
    this.id = OptionalLong.empty();
    return this;
  }

  public QuestionDefinitionBuilder setId(long id) {
    this.id = OptionalLong.of(id);
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

  public QuestionDefinitionBuilder setRepeaterId(Optional<Long> repeaterId) {
    this.repeaterId = repeaterId;
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

  public QuestionDefinitionBuilder updateQuestionText(Locale locale, String text) {
    LocalizationUtils.overwriteExistingTranslation(this.questionText, locale, text);
    return this;
  }

  public QuestionDefinitionBuilder setQuestionHelpText(
      ImmutableMap<Locale, String> questionHelpText) {
    this.questionHelpText = questionHelpText;
    return this;
  }

  public QuestionDefinitionBuilder updateQuestionHelpText(Locale locale, String helpText) {
    LocalizationUtils.overwriteExistingTranslation(this.questionHelpText, locale, helpText);
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

  public QuestionDefinitionBuilder setQuestionOptions(ImmutableList<QuestionOption> options) {
    this.questionOptions = options;
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
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            addressValidationPredicates);
      case CHECKBOX:
        MultiOptionValidationPredicates multiOptionValidationPredicates =
            MultiOptionValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          multiOptionValidationPredicates =
              MultiOptionValidationPredicates.parse(validationPredicatesString);
        }
        return new CheckboxQuestionDefinition(
            id,
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            questionOptions,
            multiOptionValidationPredicates);
      case DROPDOWN:
        return new DropdownQuestionDefinition(
            id,
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            questionOptions);
      case FILEUPLOAD:
        return new FileUploadQuestionDefinition(
            id, name, path, repeaterId, description, questionText, questionHelpText);
      case NAME:
        NameValidationPredicates nameValidationPredicates = NameValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          nameValidationPredicates = NameValidationPredicates.parse(validationPredicatesString);
        }
        return new NameQuestionDefinition(
            id,
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            nameValidationPredicates);
      case NUMBER:
        NumberQuestionDefinition.NumberValidationPredicates numberValidationPredicates =
            NumberQuestionDefinition.NumberValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          numberValidationPredicates =
              NumberQuestionDefinition.NumberValidationPredicates.parse(validationPredicatesString);
        }
        return new NumberQuestionDefinition(
            id,
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            numberValidationPredicates);
      case RADIO_BUTTON:
        return new RadioButtonQuestionDefinition(
            id,
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            questionOptions);
      case REPEATER:
        return new RepeaterQuestionDefinition(
            id, name, path, repeaterId, description, questionText, questionHelpText);
      case TEXT:
        TextValidationPredicates textValidationPredicates = TextValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          textValidationPredicates = TextValidationPredicates.parse(validationPredicatesString);
        }
        return new TextQuestionDefinition(
            id,
            name,
            path,
            repeaterId,
            description,
            questionText,
            questionHelpText,
            textValidationPredicates);
      default:
        throw new UnsupportedQuestionTypeException(this.questionType);
    }
  }
}
