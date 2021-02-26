package services.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.Program;
import services.question.QuestionDefinition;

@AutoValue
public abstract class ProgramDefinition {

  /** Unique identifier for a ProgramDefinition. */
  public abstract long id();

  /** Descriptive name of a Program, e.g. Car Tab Rebate Program */
  public abstract String name();

  /** A human readable description of a Program. */
  public abstract String description();

  /** The list of {@link BlockDefinition}s that make up the program. */
  public abstract ImmutableList<BlockDefinition> blockDefinitions();

  /** Returns the {@link QuestionDefinition} at the specified block and question indices. */
  @JsonIgnore
  public QuestionDefinition getQuestionDefinition(int blockIndex, int questionIndex) {
    return blockDefinitions().get(blockIndex).getQuestionDefinition(questionIndex);
  }

  public Optional<BlockDefinition> getBlockDefinition(long blockId) {
    return blockDefinitions().stream().filter(b -> b.id() == blockId).findAny();
  }

  public Program toProgram() {
    return new Program(this);
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_ProgramDefinition.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(long id);

    public abstract Builder setName(String name);

    public abstract Builder setDescription(String description);

    public abstract Builder setBlockDefinitions(ImmutableList<BlockDefinition> blockDefinitions);

    public abstract ImmutableList.Builder<BlockDefinition> blockDefinitionsBuilder();

    public abstract ProgramDefinition build();

    public Builder addBlockDefinition(BlockDefinition blockDefinition) {
      blockDefinitionsBuilder().add(blockDefinition);
      return this;
    }
  }
}
