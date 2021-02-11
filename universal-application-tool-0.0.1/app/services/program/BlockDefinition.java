package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.question.QuestionDefinition;

/** Defines a single program block, which contains a list of questions and data about the block. */
@AutoValue
public abstract class BlockDefinition {
  /** Name of a Block. */
  public abstract String name();

  /** Description of a Block. */
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

    public abstract Builder setQuestionDefinitions(ImmutableList<String> questionDefinitions);

    public abstract ImmutableList.Builder<QuestionDefinition> questionDefinitionsBuilder();

    public abstract BlockDefinition build();

    public Builder addQuestion(QuestionDefinition question) {
      questionDefinitionsBuilder().add(question);
      return this;
    }
  }
}
