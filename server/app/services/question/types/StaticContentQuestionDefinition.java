package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/**
 * Defines a static content question. A static content question displays static content without
 * asking for an answer.
 */
public final class StaticContentQuestionDefinition extends QuestionDefinition {

  public StaticContentQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(
                StaticContentQuestionDefinition.StaticContentValidationPredicates.create())
            .build());
  }

  public StaticContentQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      Optional<Instant> lastModifiedTime) {
    super(
        QuestionDefinitionConfig.builder()
            .setId(id)
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(
                StaticContentQuestionDefinition.StaticContentValidationPredicates.create())
            .setLastModifiedTime(lastModifiedTime)
            .build());
  }

  @AutoValue
  public abstract static class StaticContentValidationPredicates extends ValidationPredicates {

    public static StaticContentQuestionDefinition.StaticContentValidationPredicates parse(
        String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static StaticContentQuestionDefinition.StaticContentValidationPredicates create() {
      return new AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates();
    }
  }

  public StaticContentQuestionDefinition.StaticContentValidationPredicates
      getStaticContentValidationPredicates() {
    return (StaticContentQuestionDefinition.StaticContentValidationPredicates)
        getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.STATIC;
  }
}
