package services.question.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import models.QuestionDisplayMode;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionOption;
import services.question.QuestionSetting;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition.ValidationPredicates;

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

  public QuestionDefinitionBuilder setQuestionSettings(ImmutableSet<QuestionSetting> settings) {
    builder.setQuestionSettings(settings);
    return this;
  }

  public QuestionDefinitionBuilder setLastModifiedTime(Optional<Instant> lastModifiedTime) {
    builder.setLastModifiedTime(lastModifiedTime);
    return this;
  }

  public QuestionDefinitionBuilder setConcurrencyToken(UUID concurrencyToken) {
    builder.setConcurrencyToken(concurrencyToken);
    return this;
  }

  public QuestionDefinitionBuilder setDisplayMode(QuestionDisplayMode displayMode) {
    builder.setDisplayMode(displayMode);
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
    return QuestionDefinition.create(
        questionType, builder, validationPredicatesString, questionOptions, entityType);
  }
}
