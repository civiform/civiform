package services.question.types;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;

/**
 * Stores the configuration for a {@link MultiOptionQuestionDefinition}, separating the construction
 * of the attributes from the business logic of the class.
 */
@AutoValue
public abstract class MultiOptionQuestionDefinitionConfig {

  public enum MultiOptionQuestionType {
    CHECKBOX,
    DROPDOWN,
    RADIO_BUTTON
  }

  public QuestionDefinitionConfig questionDefinitionConfig() {
    return questionDefinitionConfigBuilder().build();
  }

  abstract QuestionDefinitionConfig.Builder questionDefinitionConfigBuilder();

  public abstract ImmutableList<QuestionOption> questionOptions();

  public abstract MultiOptionQuestionType multiOptionQuestionType();

  public static RequiredMultiOptionQuestionType builder() {
    return new AutoValue_MultiOptionQuestionDefinitionConfig.Builder();
  }

  static final MultiOptionValidationPredicates SINGLE_SELECT_PREDICATE =
      MultiOptionValidationPredicates.create(1, 1);

  /**
   * The method in this interface operates much like those in the {@link Builder} class, except it
   * must be set first in order to build a {@link MultiOptionQuestionDefinitionConfig}.
   */
  public interface RequiredMultiOptionQuestionType {
    Builder setMultiOptionQuestionType(MultiOptionQuestionType multiOptionQuestionType);
  }

  @AutoValue.Builder
  public abstract static class Builder implements RequiredMultiOptionQuestionType {
    abstract QuestionDefinitionConfig.Builder questionDefinitionConfigBuilder();

    abstract Builder setQuestionDefinitionConfigBuilder(QuestionDefinitionConfig.Builder builder);

    public Builder setId(long id) {
      questionDefinitionConfigBuilder().setId(id);
      return this;
    }

    public Builder setId(OptionalLong id) {
      questionDefinitionConfigBuilder().setId(id);
      return this;
    }

    public Builder setName(String name) {
      questionDefinitionConfigBuilder().setName(name);
      return this;
    }

    public Builder setEnumeratorId(long enumeratorId) {
      questionDefinitionConfigBuilder().setEnumeratorId(enumeratorId);
      return this;
    }

    public Builder setEnumeratorId(Optional<Long> enumeratorId) {
      questionDefinitionConfigBuilder().setEnumeratorId(enumeratorId);
      return this;
    }

    public Builder setDescription(String description) {
      questionDefinitionConfigBuilder().setDescription(description);
      return this;
    }

    public Builder setQuestionText(LocalizedStrings questionText) {
      questionDefinitionConfigBuilder().setQuestionText(questionText);
      return this;
    }

    public Builder setQuestionHelpText(LocalizedStrings questionHelpText) {
      questionDefinitionConfigBuilder().setQuestionHelpText(questionHelpText);
      return this;
    }

    public abstract Builder setQuestionOptions(ImmutableList<QuestionOption> questionOptions);

    public Builder setValidationPredicates(MultiOptionValidationPredicates validationPredicates) {
      questionDefinitionConfigBuilder().setValidationPredicates(validationPredicates);
      return this;
    }

    public Builder setLastModifiedTime(Instant lastModifiedTime) {
      questionDefinitionConfigBuilder().setLastModifiedTime(lastModifiedTime);
      return this;
    }

    public Builder setLastModifiedTime(Optional<Instant> lastModifiedTime) {
      questionDefinitionConfigBuilder().setLastModifiedTime(lastModifiedTime);
      return this;
    }

    abstract MultiOptionQuestionType multiOptionQuestionType();

    abstract MultiOptionQuestionDefinitionConfig autoBuild();

    public MultiOptionQuestionDefinitionConfig build() {
      if (multiOptionQuestionType() == MultiOptionQuestionType.DROPDOWN
          || multiOptionQuestionType() == MultiOptionQuestionType.RADIO_BUTTON) {
        // If we are using a dropdown or radio button, set the SINGLE_SELECT_PREDICATE to ensure
        // only one selection can be made.
        setValidationPredicates(SINGLE_SELECT_PREDICATE);
      }

      return autoBuild();
    }
  }
}
