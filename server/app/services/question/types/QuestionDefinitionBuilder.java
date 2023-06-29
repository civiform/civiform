package services.question.types;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition.AddressValidationPredicates;
import services.question.types.IdQuestionDefinition.IdValidationPredicates;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.NameQuestionDefinition.NameValidationPredicates;
import services.question.types.PhoneQuestionDefinition.PhoneValidationPredicates;
import services.question.types.QuestionDefinition.ValidationPredicates;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

/**
 * Provides helper functions to build a QuestionDefinition.
 *
 * <p>TODO(#4872): Remove this class in favor of QuestionDefinitionConfig.Builder.
 */
public final class QuestionDefinitionBuilder {

  private OptionalLong id = OptionalLong.empty();
  private String name;
  private Optional<Long> enumeratorId = Optional.empty();
  private String description;
  private LocalizedStrings questionText;
  private LocalizedStrings questionHelpText = LocalizedStrings.empty();
  private QuestionType questionType = QuestionType.TEXT;
  private String validationPredicatesString = "";
  private LocalizedStrings entityType;
  private Optional<Instant> lastModifiedTime = Optional.empty();

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
    lastModifiedTime = definition.getLastModifiedTime();

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

  public QuestionDefinitionBuilder setLastModifiedTime(Optional<Instant> lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
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
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(addressValidationPredicates)
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case CHECKBOX:
        MultiOptionValidationPredicates multiOptionValidationPredicates =
            MultiOptionValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          multiOptionValidationPredicates =
              MultiOptionValidationPredicates.parse(validationPredicatesString);
        }

        QuestionDefinitionConfig checkboxConfig =
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(multiOptionValidationPredicates)
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build();

        return new MultiOptionQuestionDefinition(
            checkboxConfig, questionOptions, MultiOptionQuestionType.CHECKBOX);

      case CURRENCY:
        return new CurrencyQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(
                    CurrencyQuestionDefinition.CurrencyValidationPredicates.create())
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case DATE:
        return new DateQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(DateQuestionDefinition.DateValidationPredicates.create())
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .setId(id)
                .build());

      case DROPDOWN:
        QuestionDefinitionConfig dropdownConfig =
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(MultiOptionValidationPredicates.create())
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build();

        return new MultiOptionQuestionDefinition(
            dropdownConfig, questionOptions, MultiOptionQuestionType.DROPDOWN);

      case EMAIL:
        return new EmailQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(EmailQuestionDefinition.EmailValidationPredicates.create())
                .setEnumeratorId(enumeratorId)
                .setId(id)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case FILEUPLOAD:
        return new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(
                    FileUploadQuestionDefinition.FileUploadValidationPredicates.create())
                .setEnumeratorId(enumeratorId)
                .setId(id)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case ID:
        IdValidationPredicates idValidationPredicates = IdValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          idValidationPredicates = IdValidationPredicates.parse(validationPredicatesString);
        }
        return new IdQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(idValidationPredicates)
                .setEnumeratorId(enumeratorId)
                .setId(id)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case NAME:
        NameValidationPredicates nameValidationPredicates = NameValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          nameValidationPredicates = NameValidationPredicates.parse(validationPredicatesString);
        }
        return new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(nameValidationPredicates)
                .setEnumeratorId(enumeratorId)
                .setId(id)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case NUMBER:
        NumberQuestionDefinition.NumberValidationPredicates numberValidationPredicates =
            NumberQuestionDefinition.NumberValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          numberValidationPredicates =
              NumberQuestionDefinition.NumberValidationPredicates.parse(validationPredicatesString);
        }
        return new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(numberValidationPredicates)
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case RADIO_BUTTON:
        QuestionDefinitionConfig radioButtonConfig =
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(MultiOptionValidationPredicates.create())
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build();

        return new MultiOptionQuestionDefinition(
            radioButtonConfig, questionOptions, MultiOptionQuestionType.RADIO_BUTTON);

      case ENUMERATOR:
        // This shouldn't happen, but protects us in case there are enumerator questions in the prod
        // database that don't have entity type specified.
        if (entityType == null || entityType.isEmpty()) {
          entityType =
              LocalizedStrings.withDefaultValue(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
        }
        return new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(
                    EnumeratorQuestionDefinition.EnumeratorValidationPredicates.create())
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build(),
            entityType);

      case STATIC:
        return new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(
                    StaticContentQuestionDefinition.StaticContentValidationPredicates.create())
                .setId(id)
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .build());

      case TEXT:
        TextValidationPredicates textValidationPredicates = TextValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          textValidationPredicates = TextValidationPredicates.parse(validationPredicatesString);
        }
        return new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(TextQuestionDefinition.TextValidationPredicates.create())
                .setEnumeratorId(enumeratorId)
                .setLastModifiedTime(lastModifiedTime)
                .setValidationPredicates(textValidationPredicates)
                .setId(id)
                .build());
      case PHONE:
        PhoneValidationPredicates phoneValidationPredicates = PhoneValidationPredicates.create();
        if (!validationPredicatesString.isEmpty()) {
          phoneValidationPredicates = PhoneValidationPredicates.parse(validationPredicatesString);
        }
        return new PhoneQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(description)
                .setQuestionText(questionText)
                .setQuestionHelpText(questionHelpText)
                .setValidationPredicates(phoneValidationPredicates)
                .setEnumeratorId(enumeratorId)
                .setId(id)
                .setLastModifiedTime(lastModifiedTime)
                .build());
      default:
        throw new UnsupportedQuestionTypeException(this.questionType);
    }
  }
}
