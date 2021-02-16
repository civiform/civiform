package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import models.Program;

@AutoValue
public abstract class ProgramDefinition {

  /** Unique identifier for a ProgramDefinition. */
  @Nullable
  public abstract Long id();

  /** Descriptive name of a Program, e.g. Car Tab Rebate Program */
  public abstract String name();

  /** A human readable description of a Program. */
  public abstract String description();

  /** The list of {@link BlockDefinitions} that make up the program. */
  public abstract ImmutableList<BlockDefinition> blockDefinitions();

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

  public Program toProgram() {
    return new Program(this);
  }
}
