package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import com.google.common.collect.ImmutableList;
import services.program.BlockDefinition;

/**
 * Used only for serializing a {@link services.program.ProgramDefinition}'s list of blocks to the
 * database.
 */
public class BlockContainer {

  private final ImmutableList<String> blockDefinitions;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public BlockContainer(@JsonProperty("blockDefinitions") ImmutableList<String> blockDefinitions) {
    this.blockDefinitions = blockDefinitions;
  }

  public ImmutableList<String> blockDefinitions() {
    return this.blockDefinitions;
  }
}
