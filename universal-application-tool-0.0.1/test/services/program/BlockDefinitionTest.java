package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class BlockDefinitionTest {
  @Test
  public void createBlockDefinition() {
    BlockDefinition block =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    assertThat(block.id()).isEqualTo(123L);
  }
}
