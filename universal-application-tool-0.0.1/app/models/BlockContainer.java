package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class BlockContainer {

  @JsonCreator
  public static BlockContainer create(
      @JsonProperty("blockDefinitions") ImmutableList<String> blockDefinitions) {
    return new AutoValue_BlockContainer(blockDefinitions);
  }

  @JsonProperty("blockDefinitions")
  public abstract ImmutableList<String> blockDefinitions();
}
