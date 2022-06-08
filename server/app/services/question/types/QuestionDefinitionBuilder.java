package services.question.types;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition.AddressValidationPredicates;
import services.question.types.IdQuestionDefinition.IdValidationPredicates;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.NameQuestionDefinition.NameValidationPredicates;
import services.question.types.QuestionDefinition.ValidationPredicates;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

/** Provides helper functions to build a QuestionDefinition. */
public class QuestionDefinitionBuilder {

  private OptionalLong id = OptionalLong.empty();
  private String name;
  private Optional<Long> enumeratorId = Optional.empty();
  private String description;
  private LocalizedStrings questionText;
  private LocalizedStrings questionHelpText = LocalizedStrings.empty();
  private QuestionType questionType = QuestionType.TEXT;
  private String validationPredicatesString = "";
  private LocalizedStrings entityType;

  // Multi-option question types only.
  private ImmutableList<QuestionOption> questionOptions = ImmutableList.of();

  public QuestionDefinitionBuilder() {}

  public QuestionDefinitionBuilder(QuestionDefinition definition) {
    if (definition.isPersisted()) {
      long definitionId = definition.getId();
      this.id = OptionalLong.of(definitionId);
    }
    name = definition.getName();
    enumeratorId = definition.getEnumeratorId();
    description = definition.getDescription();
    questionText = definition.getQuestionText();
    questionHelpText = definition.getQuestionHelpText();
    questionType = definition.getQuestionType();
    validationPredicatesString = definition.getValidationPredicatesAsString();

    if (definition.getQuestionType().equals(QuestionType.ENUMERATOR)) {
      EnumeratorQuestionDefinition enumeratorQuestionDefinition =
          (EnumeratorQuestionDefinition) definition;
      entityType = enumeratorQuestionDefinition.getEntityType();
    }

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
            .setQuestionText(LocalizedStrings.of(Locale.US, "Sample question text"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "Sample question help text"))
            .setQuestionType(questionType);

    if (questionType.isMultiOptionType()) {
      builder.setQuestionOptions(
          ImmutableList.of(
              QuestionOption.create(
                  1L, 1L, LocalizedStrings.of(Locale.US, "Sample question option"))));
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

  public QuestionDefinitionBuilder setEnumeratorId(Optional<Long> enumeratorId) {
    this.enumeratorId = enumeratorId;
    return this;
  }

  public QuestionDefinitionBuilder setEntityType(LocalizedStrings entityType) {
    this.entityType = entityType;
    return this;
  }

  public QuestionDefinitionBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public QuestionDefinitionBuilder setQuestionText(LocalizedStrings questionText) {
    this.questionText = questionText;
    return this;
  }

  public QuestionDefinitionBuilder updateQuestionText(Locale locale, String text) {
    questionText = questionText.updateTranslation(locale, text);
    return this;
  }

  public QuestionDefinitionBuilder setQuestionHelpText(LocalizedStrings questionHelpText) {
    this.questionHelpText = questionHelpText;
    return this;
  }

  public QuestionDefinitionBuilder updateQuestionHelpText(Locale locale, String helpText) {
    questionHelpText = questionHelpText.updateTranslation(locale, helpText);
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

  /**
   * Calls {@code build} and throws a {@link RuntimeException} if the {@link QuestionType} is
   * invalid.
   */
  public QuestionDefinition unsafeBuild() {
    try {
      return build();
    } catch (UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }
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
            enumeratorId,
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
            enumeratorId,
            description,
            questionText,
            questionHelpText,
            questionOptions,
            multiOptionValidationPredicates);

      case CURRENCY:
        return new CurrencyQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText);

      case DATE:
        return new DateQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText);

      case DROPDOWN:
        return new DropdownQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText, questionOptions);

      case EMAIL:
        return new EmailQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText);

      case FILEUPLOAD:
        return new FileUploadQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText);

      case ID:
        IdValidationPredicates idValidationPredicates = IdValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          idValidationPredicates = IdValidationPredicates.parse(validationPredicatesString);
        }
        return new IdQuestionDefinition(
            id,
            name,
            enumeratorId,
            description,
            questionText,
            questionHelpText,
            idValidationPredicates);

      case NAME:
        NameValidationPredicates nameValidationPredicates = NameValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          nameValidationPredicates = NameValidationPredicates.parse(validationPredicatesString);
        }
        return new NameQuestionDefinition(
            id,
            name,
            enumeratorId,
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
            enumeratorId,
            description,
            questionText,
            questionHelpText,
            numberValidationPredicates);

      case RADIO_BUTTON:
        return new RadioButtonQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText, questionOptions);

      case ENUMERATOR:
        // This shouldn't happen, but protects us in case there are enumerator questions in the prod
        // database that don't have entity type specified.
        if (entityType == null || entityType.isEmpty()) {
          entityType =
              LocalizedStrings.withDefaultValue(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
        }
        return new EnumeratorQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText, entityType);

      case STATIC:
        return new StaticContentQuestionDefinition(
            id, name, enumeratorId, description, questionText, questionHelpText);

      case TEXT:
        TextValidationPredicates textValidationPredicates = TextValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          textValidationPredicates = TextValidationPredicates.parse(validationPredicatesString);
        }
        return new TextQuestionDefinition(
            id,
            name,
            enumeratorId,
            description,
            questionText,
            questionHelpText,
            textValidationPredicates);

      default:
        throw new UnsupportedQuestionTypeException(this.questionType);
    }
  }
}
