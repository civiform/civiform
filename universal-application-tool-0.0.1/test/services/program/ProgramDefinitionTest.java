package services.program;

import org.junit.Test;

public class ProgramDefinitionTest {
  @Test
  public void createProgramDefinition() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition.builder()
        .setId(123L)
        .setName("The Program")
        .setDescription("This program is for testing.")
        .addBlockDefinition(blockA)
        .build();
  }
}
