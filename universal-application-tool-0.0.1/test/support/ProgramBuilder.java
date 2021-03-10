package support;

import com.google.common.collect.ImmutableList;
import models.Program;
import services.program.ProgramDefinition;

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

  public static ProgramBuilder newProgram() {
    Program program = new Program("", "");
    program.save();
    return new ProgramBuilder(
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of()));
  }

  public static ProgramBuilder newProgram(String name) {
    Program program = new Program(name, "");
    program.save();
    return new ProgramBuilder(
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of()));
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

  public BlockBuilder withBlock(String name) {
    long blockId = Long.valueOf(++numBlocks);
    return BlockBuilder.newBlock(this, blockId, name);
  }

  public BlockBuilder withBlock(String name, String description) {
    long blockId = Long.valueOf(++numBlocks);
    return BlockBuilder.newBlock(this, blockId, name, description);
  }

  public ProgramDefinition buildDefinition() {
    Program program = build();
    return program.getProgramDefinition();
  }

  public Program build() {
    ProgramDefinition programDefinition = builder.build();
    if (programDefinition.blockDefinitions().isEmpty()) {
      return withBlock().build();
    }

    Program program = programDefinition.toProgram();
    program.update();
    return program;
  }
}
