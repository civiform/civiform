package services.program;

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
  }
}
