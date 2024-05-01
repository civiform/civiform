package services.question.types;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;

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

  LocalizedStrings questionHelpText() {
    return questionHelpTextInternal().orElse(LocalizedStrings.empty());
  }

  abstract Optional<LocalizedStrings> questionHelpTextInternal();

  abstract Optional<QuestionDefinition.ValidationPredicates> validationPredicates();

  abstract OptionalLong id();

  abstract Optional<Long> enumeratorId();

  abstract Optional<Instant> lastModifiedTime();

  abstract boolean universal();

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
    RequiredDescription setName(String name);
  }

  public interface RequiredDescription {
    RequiredQuestionText setDescription(String description);
  }

  public interface RequiredQuestionText {
    Builder setQuestionText(LocalizedStrings questionText);
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements RequiredName, RequiredDescription, RequiredQuestionText {
    public abstract Builder setId(long id);

    public abstract Builder setId(OptionalLong id);

    public abstract Builder setEnumeratorId(long enumeratorId);

    public abstract Builder setEnumeratorId(Optional<Long> enumeratorId);

    public abstract Builder setLastModifiedTime(Instant lastModifiedTime);

    public abstract Builder setLastModifiedTime(Optional<Instant> lastModifiedTime);

    public abstract Builder setValidationPredicates(
        QuestionDefinition.ValidationPredicates validationPredicates);

    public Builder setQuestionHelpText(LocalizedStrings questionHelpText) {
      return setQuestionHelpTextInternal(questionHelpText);
    }

    abstract Builder setQuestionHelpTextInternal(LocalizedStrings questionHelpText);

    public abstract Builder setUniversal(boolean universal);

    public abstract Builder setPrimaryApplicantInfoTags(
        ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags);

    public abstract QuestionDefinitionConfig build();
  }
}
