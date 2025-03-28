package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;

@AutoValue
@JsonDeserialize(builder = AutoValue_QuestionDefinitionConfig.Builder.class)
// The JsonInclude.Include.NON_ABSENT annotation tells Jackson to only include optional fields if
// they are not Optional.empty. This is required so that Jackson does not deserialize empty
// enumeratorId fields to the number 0 which our code reads as an actual enumerator id.
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class QuestionDefinitionConfig {

  @JsonProperty("name")
  abstract String name();

  @JsonProperty("description")
  abstract String description();

  // Note: you must check prefixes anytime you are doing a locale lookup
  // see getQuestionText body comment for explanation.

  /**
   * Note: you must check prefixes anytime you are doing a locale lookup see {@link
   * QuestionDefinition#getQuestionText} body comment for explanation.
   */
  @JsonProperty("questionText")
  abstract LocalizedStrings questionText();

  @JsonIgnore
  LocalizedStrings questionHelpText() {
    return questionHelpTextInternal().orElse(LocalizedStrings.empty());
  }

  @JsonProperty("questionHelpText")
  abstract Optional<LocalizedStrings> questionHelpTextInternal();

  @JsonProperty("validationPredicates")
  abstract Optional<QuestionDefinition.ValidationPredicates> validationPredicates();

  @JsonProperty("id")
  abstract OptionalLong id();

  @JsonProperty("enumeratorId")
  abstract Optional<Long> enumeratorId();

  @JsonIgnore
  abstract Optional<Instant> lastModifiedTime();

  @JsonIgnore
  abstract Optional<UUID> concurrencyToken();

  @JsonProperty("universal")
  abstract boolean universal();

  @JsonProperty("primaryApplicantInfoTags")
  abstract ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags();

  /**
   * Used to create a new {@link QuestionDefinitionConfig}. We default some fields here to avoid
   * having to explicitly set default values everywhere that is using a builder and not trying to
   * use these fields.
   */
  public static RequiredName builder() {
    return new AutoValue_QuestionDefinitionConfig.Builder()
        .setUniversal(false)
        .setPrimaryApplicantInfoTags(ImmutableSet.of());
  }

  /** Used to create a new {@link QuestionDefinitionConfig.Builder} based on an existing one. */
  public abstract Builder toBuilder();

  public interface RequiredName {
    @JsonProperty("name")
    RequiredDescription setName(String name);
  }

  public interface RequiredDescription {
    @JsonProperty("description")
    RequiredQuestionText setDescription(String description);
  }

  public interface RequiredQuestionText {
    @JsonProperty("questionText")
    Builder setQuestionText(LocalizedStrings questionText);
  }

  // This is a step builder:
  // https://github.com/google/auto/blob/main/value/userguide/builders-howto.md#-create-a-step-builder
  @AutoValue.Builder
  public abstract static class Builder
      implements RequiredName, RequiredDescription, RequiredQuestionText {
    @JsonProperty("id")
    public abstract Builder setId(long id);

    @JsonProperty("id")
    public abstract Builder setId(OptionalLong id);

    @JsonProperty("enumeratorId")
    public abstract Builder setEnumeratorId(long enumeratorId);

    @JsonProperty("enumeratorId")
    public abstract Builder setEnumeratorId(Optional<Long> enumeratorId);

    public abstract Builder setLastModifiedTime(Instant lastModifiedTime);

    public abstract Builder setLastModifiedTime(Optional<Instant> lastModifiedTime);

    public abstract Builder setConcurrencyToken(UUID concurrencyToken);

    @JsonProperty("validationPredicates")
    public abstract Builder setValidationPredicates(
        QuestionDefinition.ValidationPredicates validationPredicates);

    public Builder setQuestionHelpText(LocalizedStrings questionHelpText) {
      return setQuestionHelpTextInternal(questionHelpText);
    }

    @JsonProperty("questionHelpText")
    abstract Builder setQuestionHelpTextInternal(LocalizedStrings questionHelpText);

    @JsonProperty("universal")
    public abstract Builder setUniversal(boolean universal);

    @JsonProperty("primaryApplicantInfoTags")
    public abstract Builder setPrimaryApplicantInfoTags(
        ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags);

    public abstract QuestionDefinitionConfig build();
  }
}
