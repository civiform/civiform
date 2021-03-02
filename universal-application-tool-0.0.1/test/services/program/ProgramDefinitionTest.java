package services.program;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Test
  public void getBlockDefinition_hasValue() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setName("The Program")
            .setDescription("This program is for testing.")
            .addBlockDefinition(blockA)
            .build();

    assertThat(program.getBlockDefinition(0)).hasValue(blockA);
  }

  @Test
  public void getBlockDefinition_outOfBounds_isEmpty() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setName("The Program")
            .setDescription("This program is for testing.")
            .build();

    assertThat(program.getBlockDefinition(0)).isEmpty();
  }
}
