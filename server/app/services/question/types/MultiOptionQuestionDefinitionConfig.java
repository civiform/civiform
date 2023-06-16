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

  abstract OptionalLong id();

  abstract String name();

  abstract Optional<Long> enumeratorId();

  abstract String description();

  abstract LocalizedStrings questionText();

  abstract LocalizedStrings questionHelpText();

  abstract ImmutableList<QuestionOption> questionOptions();

  abstract Optional<MultiOptionValidationPredicates> validationPredicates();

  abstract Optional<Instant> lastModifiedTime();

  abstract MultiOptionQuestionType multiOptionQuestionType();

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
    public abstract Builder setId(long id);

    public abstract Builder setId(OptionalLong id);

    public abstract Builder setName(String name);

    public abstract Builder setEnumeratorId(long enumeratorId);

    public abstract Builder setEnumeratorId(Optional<Long> enumeratorId);

    public abstract Builder setDescription(String description);

    public abstract Builder setQuestionText(LocalizedStrings questionText);

    public abstract Builder setQuestionHelpText(LocalizedStrings questionHelpText);

    public abstract Builder setQuestionOptions(ImmutableList<QuestionOption> questionOptions);

    public abstract Builder setValidationPredicates(
        MultiOptionValidationPredicates validationPredicates);

    public abstract Builder setLastModifiedTime(Instant lastModifiedTime);

    public abstract Builder setLastModifiedTime(Optional<Instant> lastModifiedTime);

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
