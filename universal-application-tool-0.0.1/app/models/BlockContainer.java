package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.program.BlockDefinition;

/**
 * A wrapper for {@link BlockDefinition}s for use in serializing to JSON in the {@link
 * models.Program} model.
 */
@AutoValue
public abstract class BlockContainer {

  @JsonCreator
  public static BlockContainer create(
      @JsonProperty("blockDefinitions") ImmutableList<BlockDefinition> blockDefinitions) {
    return new AutoValue_BlockContainer(blockDefinitions);
  }

  @JsonProperty("blockDefinitions")
  public abstract ImmutableList<BlockDefinition> blockDefinitions();
}
