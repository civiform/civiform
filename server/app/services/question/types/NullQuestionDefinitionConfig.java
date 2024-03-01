package services.question.types;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;

/**
 * This is an empty QuestionDefinitionConfig that is used when the system can't find the question.
 * It works with the NullQuestionDefinition class to provide a placeholder object that assists in
 * letting the application to continue loading in the event of an error.
 *
 * <p>In normal operation this shouldn't ever be reached, but an occasional gremlin has resulted in
 * the rare instance of a program pointing at an old version of a question.
 */
public class NullQuestionDefinitionConfig extends QuestionDefinitionConfig {
  private final long id;

  public NullQuestionDefinitionConfig(long id) {
    this.id = id;
  }

  @Override
  String name() {
    return "null-name-" + id;
  }

  @Override
  String description() {
    return "null-description-" + id;
  }

  /**
   * Note: you must check prefixes anytime you are doing a locale lookup see {@link
   * QuestionDefinition#getQuestionText} body comment for explanation.
   */
  @Override
  LocalizedStrings questionText() {
    return LocalizedStrings.of(Locale.US, "en-US");
  }

  @Override
  Optional<LocalizedStrings> questionHelpTextInternal() {
    return Optional.empty();
  }

  @Override
  Optional<QuestionDefinition.ValidationPredicates> validationPredicates() {
    return Optional.empty();
  }

  @Override
  OptionalLong id() {
    return OptionalLong.of(id);
  }

  @Override
  Optional<Long> enumeratorId() {
    return Optional.empty();
  }

  @Override
  Optional<Instant> lastModifiedTime() {
    return Optional.empty();
  }

  @Override
  boolean universal() {
    return false;
  }

  @Override
  ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags() {
    return ImmutableSet.of();
  }

  /** Used to create a new {@link Builder} based on an existing one. */
  public static class Builder extends QuestionDefinitionConfig.Builder {

    private final NullQuestionDefinitionConfig _nullQuestionDefinitionConfig;

    public Builder(NullQuestionDefinitionConfig nullQuestionDefinitionConfig) {
      _nullQuestionDefinitionConfig = nullQuestionDefinitionConfig;
    }

    @Override
    public RequiredDescription setName(String name) {
      return description -> questionText -> this;
    }

    @Override
    public RequiredQuestionText setDescription(String description) {
      return questionText -> this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setQuestionText(LocalizedStrings questionText) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setId(long id) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setId(OptionalLong id) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setEnumeratorId(long enumeratorId) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setEnumeratorId(Optional<Long> enumeratorId) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setLastModifiedTime(Instant lastModifiedTime) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setLastModifiedTime(
        Optional<Instant> lastModifiedTime) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setValidationPredicates(
        QuestionDefinition.ValidationPredicates validationPredicates) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setUniversal(boolean Universal) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setPrimaryApplicantInfoTags(
        ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags) {
      return this;
    }

    @Override
    QuestionDefinitionConfig.Builder setQuestionHelpTextInternal(
        LocalizedStrings questionHelpText) {
      return this;
    }

    @Override
    public QuestionDefinitionConfig build() {
      return _nullQuestionDefinitionConfig;
    }
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }
}
