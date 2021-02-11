package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.question.QuestionDefinition;

/**
 * Defines a single program block, which contains a list of questions and data about the block.
 * Blocks are displayed to applicants one per-page, and are the primary means by which applicants
 * navigate within a program form.
 */
@AutoValue
public abstract class BlockDefinition {

  /**
   * Name of a Block used to identify it to the admin. The name is only visible to the admin so is
   * not localized.
   */
  public abstract String name();

  /**
   * A human readable description of the Block. The description is only visible to the admin so is
   * not localized.
   */
  public abstract String description();

  /** A {@link Predicate} that determines whether this is hidden or shown. */
  public abstract Optional<Predicate> hidePredicate();

  /** A {@link Predicate} that determines whether this is optional or required. */
  public abstract Optional<Predicate> optionalPredicate();

  /** The list of {@link QuestionDefinition}s that make up this block. */
  public abstract ImmutableList<QuestionDefinition> questionDefinitions();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_BlockDefinition.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    public abstract Builder setName(String value);

    public abstract Builder setDescription(String value);

    public abstract Builder setHidePredicate(Optional<Predicate> hide);

    public abstract Builder setOptionalPredicate(Optional<Predicate> optional);

    public abstract Builder setQuestionDefinitions(
        ImmutableList<QuestionDefinition> questionDefinitions);

    public abstract ImmutableList.Builder<QuestionDefinition> questionDefinitionsBuilder();

    public abstract BlockDefinition build();

    public Builder addQuestion(QuestionDefinition question) {
      questionDefinitionsBuilder().add(question);
      return this;
    }
  }
}
