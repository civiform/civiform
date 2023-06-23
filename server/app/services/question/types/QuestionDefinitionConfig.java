package services.question.types;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

@AutoValue
public abstract class QuestionDefinitionConfig {

  abstract OptionalLong id();

  abstract String name();

  abstract Optional<Long> enumeratorId();

  abstract String description();

  // Note: you must check prefixes anytime you are doing a locale lookup
  // see getQuestionText body comment for explanation.
  abstract LocalizedStrings questionText();

  abstract LocalizedStrings questionHelpText();

  abstract QuestionDefinition.ValidationPredicates validationPredicates();

  abstract Optional<Instant> lastModifiedTime();

  public static Builder builder() {
    return new AutoValue_QuestionDefinitionConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(long id);

    public abstract Builder setId(OptionalLong id);

    public abstract Builder setName(String name);

    public abstract Builder setEnumeratorId(long enumeratorId);

    public abstract Builder setEnumeratorId(Optional<Long> enumeratorId);

    public abstract Builder setDescription(String description);

    public abstract Builder setQuestionText(LocalizedStrings questionText);

    public abstract Builder setQuestionHelpText(LocalizedStrings questionHelpText);

    public abstract Builder setValidationPredicates(
        QuestionDefinition.ValidationPredicates validationPredicates);

    public abstract Builder setLastModifiedTime(Instant lastModifiedTime);

    public abstract Builder setLastModifiedTime(Optional<Instant> lastModifiedTime);

    public abstract QuestionDefinitionConfig build();
  }
}
