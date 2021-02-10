package services.program;

import org.junit.Test;

public class BlockDefinitionTest {
  @Test
  public void createBlockDefinition() {
    BlockDefinition block =
        BlockDefinition.builder().setName("Block Name").setDescription("Block Description").build();
  }
}
