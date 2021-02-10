package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class ProgramDefinition {
  public abstract String id();

  public abstract String name();

  public abstract String description();

  public abstract ImmutableList<BlockDefinition> blockDefinitions();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_ProgramDefinition.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    public abstract Builder setId(String id);

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
