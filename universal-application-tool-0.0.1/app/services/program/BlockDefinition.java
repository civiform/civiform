package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Defines a single program block, which contains a list of questions and data about the block. */
@AutoValue
public abstract class BlockDefinition {

  public abstract String name();

  public abstract String description();

  public abstract Optional<Predicate> hidePredicate();

  public abstract Optional<Predicate> optionalPredicate();

  public abstract ImmutableList<String> questionDefinitions();

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

    public abstract ImmutableList.Builder<String> questionDefinitionsBuilder();

    public abstract BlockDefinition build();

    public Builder addQuestion(String question) {
      questionDefinitionsBuilder().add(question);
      return this;
    }
  }
}
