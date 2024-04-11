package services.question.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import play.libs.Json;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition.AddressValidationPredicates;
import services.question.types.IdQuestionDefinition.IdValidationPredicates;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.NameQuestionDefinition.NameValidationPredicates;
import services.question.types.NumberQuestionDefinition.NumberValidationPredicates;
import services.question.types.PhoneQuestionDefinition.PhoneValidationPredicates;
import services.question.types.QuestionDefinition.ValidationPredicates;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

/**
 * DEPRECATED. Provides helper functions to build a {@link QuestionDefinition}.
 *
 * <p>TODO(#5271): Remove this class in favor of {@link QuestionDefinitionConfig.Builder}.
 */
public final class QuestionDefinitionBuilder {

  /**
   * The {@link QuestionDefinitionConfig.Builder} is the basis for this class. This class
   * essentially operates as a wrapper around it; this is technical debt, and all usages should
   * eventually directly use the {@link QuestionDefinitionConfig.Builder} instead.
   */
  private final QuestionDefinitionConfig.Builder builder;

  // Additional per-question fields.
  private ImmutableList<QuestionOption> questionOptions = ImmutableList.of();
  private String validationPredicatesString = "";
  private QuestionType questionType;
  private LocalizedStrings entityType;

  public QuestionDefinitionBuilder() {
    // Cast the builder in order to avoid the "required" methods such as
    // QuestionDefinitionConfig.RequiredName.
    // This is appropriate only in this class, since it is itself a builder.
    builder = (QuestionDefinitionConfig.Builder) QuestionDefinitionConfig.builder();
  }

  public QuestionDefinitionBuilder(QuestionDefinition definition) {
    this.builder = definition.getConfig().toBuilder();

    if (definition.isPersisted()) {
      long definitionId = definition.getId();
      this.builder.setId(definitionId);
    }

    validationPredicatesString = definition.getValidationPredicatesAsString();
    questionType = definition.getQuestionType();

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
    builder.setId(OptionalLong.empty());
    return this;
  }

  public QuestionDefinitionBuilder setId(Void v) {
    builder.setId(OptionalLong.empty());
    return this;
  }

  public QuestionDefinitionBuilder setId(long id) {
    builder.setId(id);
    return this;
  }

  public QuestionDefinitionBuilder setName(String name) {
    builder.setName(name);
    return this;
  }

  public QuestionDefinitionBuilder setEnumeratorId(Optional<Long> enumeratorId) {
    builder.setEnumeratorId(enumeratorId);
    return this;
  }

  public QuestionDefinitionBuilder setEntityType(LocalizedStrings entityType) {
    this.entityType = entityType;
    return this;
  }

  public QuestionDefinitionBuilder setDescription(String description) {
    builder.setDescription(description);
    return this;
  }

  public QuestionDefinitionBuilder setQuestionText(LocalizedStrings questionText) {
    builder.setQuestionText(questionText);
    return this;
  }

  public QuestionDefinitionBuilder updateQuestionText(Locale locale, String text) {
    builder.setQuestionText(builder.build().questionText().updateTranslation(locale, text));
    return this;
  }

  public QuestionDefinitionBuilder setQuestionHelpText(LocalizedStrings questionHelpText) {
    builder.setQuestionHelpText(questionHelpText);
    return this;
  }

  public QuestionDefinitionBuilder updateQuestionHelpText(Locale locale, String helpText) {
    builder.setQuestionHelpText(
        builder.build().questionHelpText().updateTranslation(locale, helpText));
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
    builder.setLastModifiedTime(lastModifiedTime);
    return this;
  }

  public QuestionDefinitionBuilder setUniversal(boolean universal) {
    builder.setUniversal(universal);
    return this;
  }

  public QuestionDefinitionBuilder setPrimaryApplicantInfoTags(
      ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags) {
    builder.setPrimaryApplicantInfoTags(primaryApplicantInfoTags);
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
    String reformattedValidationPredicatesString = createValidationPredicatesWithType();

    switch (this.questionType) {
      case ADDRESS:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              AddressValidationPredicates.parse(reformattedValidationPredicatesString));
        }
        return new AddressQuestionDefinition(builder.build());

      case CHECKBOX:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              MultiOptionValidationPredicates.parse(reformattedValidationPredicatesString));
        }

        return new MultiOptionQuestionDefinition(
            builder.build(), questionOptions, MultiOptionQuestionType.CHECKBOX);

      case CURRENCY:
        return new CurrencyQuestionDefinition(builder.build());

      case DATE:
        return new DateQuestionDefinition(builder.build());

      case DROPDOWN:
        return new MultiOptionQuestionDefinition(
            builder.build(), questionOptions, MultiOptionQuestionType.DROPDOWN);

      case EMAIL:
        return new EmailQuestionDefinition(builder.build());

      case FILEUPLOAD:
        return new FileUploadQuestionDefinition(builder.build());

      case ID:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              IdValidationPredicates.parse(reformattedValidationPredicatesString));
        }
        return new IdQuestionDefinition(builder.build());

      case NAME:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              NameValidationPredicates.parse(reformattedValidationPredicatesString));
        }
        return new NameQuestionDefinition(builder.build());

      case NUMBER:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              NumberValidationPredicates.parse(reformattedValidationPredicatesString));
        }
        return new NumberQuestionDefinition(builder.build());

      case RADIO_BUTTON:
        return new MultiOptionQuestionDefinition(
            builder.build(), questionOptions, MultiOptionQuestionType.RADIO_BUTTON);

      case ENUMERATOR:
        // This shouldn't happen, but protects us in case there are enumerator questions in the prod
        // database that don't have entity type specified.
        if (entityType == null || entityType.isEmpty()) {
          entityType =
              LocalizedStrings.withDefaultValue(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
        }
        return new EnumeratorQuestionDefinition(builder.build(), entityType);

      case STATIC:
        return new StaticContentQuestionDefinition(builder.build());

      case TEXT:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              TextValidationPredicates.parse(reformattedValidationPredicatesString));
        }
        return new TextQuestionDefinition(builder.build());

      case PHONE:
        if (!reformattedValidationPredicatesString.isEmpty()) {
          builder.setValidationPredicates(
              PhoneValidationPredicates.parse(reformattedValidationPredicatesString));
        }
        return new PhoneQuestionDefinition(builder.build());

      default:
        throw new UnsupportedQuestionTypeException(this.questionType);
    }
  }

  private String createValidationPredicatesWithType() {
    JsonNode parsed = Json.parse(validationPredicatesString);
    if (parsed.isEmpty()) {
      return "";
    }

    if (parsed.findPath("type").isEmpty()) {
      String type = this.questionType.name().toLowerCase(Locale.getDefault());
      if (this.questionType == QuestionType.CHECKBOX
          || this.questionType == QuestionType.RADIO_BUTTON
          || this.questionType == QuestionType.DROPDOWN) {
        type = "multioption";
      }
      ObjectNode newNode = (ObjectNode) parsed;
      newNode.put("type", type);
      return newNode.toString();
    }

    return validationPredicatesString;
  }
}
