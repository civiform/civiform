package support;

import com.google.common.collect.ImmutableList;
import models.Program;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;

public class ProgramBuilder {

  ProgramDefinition.Builder builder;
  int numBlocks = 0;

  private ProgramBuilder(ProgramDefinition.Builder builder) {
    this.builder = builder;
  }

  private ProgramBuilder(ProgramDefinition.Builder builder, int numBlocks) {
    this.builder = builder;
    this.numBlocks = numBlocks;
  }

  public static ProgramBuilder newProgram(String name, String description) {
    Program program = new Program(name, description);
    program.save();
    return new ProgramBuilder(
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of()));
  }

  public ProgramBuilder withName(String name) {
    builder.setName(name);
    return this;
  }

  public ProgramBuilder withDescription(String description) {
    builder.setDescription(description);
    return this;
  }

  public BlockBuilder withBlock() {
    long blockId = Long.valueOf(++numBlocks);
    return BlockBuilder.newBlock(this, blockId);
  }

  public BlockBuilder withBlock(String name, String description) {
    long blockId = Long.valueOf(++numBlocks);
    return BlockBuilder.newBlock(this, blockId, name, description);
  }

  public ProgramDefinition build() throws ProgramNeedsABlockException {
    ProgramDefinition programDefinition = builder.build();
    if (programDefinition.blockDefinitions().isEmpty()) {
      throw new ProgramNeedsABlockException(programDefinition.id());
    }
    programDefinition.toProgram().save();
    return programDefinition;
  }
}
