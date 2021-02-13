package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.program.BlockDefinition;

@AutoValue
public abstract class BlockContainer {

  @JsonCreator
  public static BlockContainer create(
      @JsonProperty("blockDefinitions") ImmutableList<String> blockDefinitions) {
    return new AutoValue_BlockContainer(blockDefinitions);
  }

  public abstract ImmutableList<String> blockDefinitions();
}
