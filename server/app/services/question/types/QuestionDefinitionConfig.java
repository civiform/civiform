package services.question.types;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

@AutoValue
public abstract class QuestionDefinitionConfig {

  abstract String name();

  abstract String description();

  // Note: you must check prefixes anytime you are doing a locale lookup
  // see getQuestionText body comment for explanation.

  /**
   * Note: you must check prefixes anytime you are doing a locale lookup see {@link
   * QuestionDefinition#getQuestionText} body comment for explanation.
   */
  abstract LocalizedStrings questionText();

  abstract LocalizedStrings questionHelpText();

  abstract Optional<QuestionDefinition.ValidationPredicates> validationPredicates();

  abstract OptionalLong id();

  abstract Optional<Long> enumeratorId();

  abstract Optional<Instant> lastModifiedTime();

  /** Used to create a new {@link QuestionDefinitionConfig} */
  public static RequiredName builder() {
    return new AutoValue_QuestionDefinitionConfig.Builder();
  }

  /** Used to create a new {@link QuestionDefinitionConfig.Builder} based on an existing one. */
  public abstract Builder toBuilder();

  public interface RequiredName {
    RequiredDescription setName(String name);
  }

  public interface RequiredDescription {
    RequiredQuestionText setDescription(String description);
  }

  public interface RequiredQuestionText {
    RequiredQuestionHelpText setQuestionText(LocalizedStrings questionText);
  }

  public interface RequiredQuestionHelpText {
    Builder setQuestionHelpText(LocalizedStrings questionHelpText);
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements RequiredName, RequiredDescription, RequiredQuestionText, RequiredQuestionHelpText {
    public abstract Builder setId(long id);

    public abstract Builder setId(OptionalLong id);

    public abstract Builder setEnumeratorId(long enumeratorId);

    public abstract Builder setEnumeratorId(Optional<Long> enumeratorId);

    public abstract Builder setLastModifiedTime(Instant lastModifiedTime);

    public abstract Builder setLastModifiedTime(Optional<Instant> lastModifiedTime);

    public abstract Builder setValidationPredicates(
        QuestionDefinition.ValidationPredicates validationPredicates);

    public abstract QuestionDefinitionConfig build();
  }
}
