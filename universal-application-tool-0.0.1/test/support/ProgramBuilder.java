package support;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.PersistenceException;
import models.LifecycleStage;
import models.Program;
import models.Question;
import services.program.BlockDefinition;
import services.program.ExportDefinition;
import services.program.Predicate;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;

public class ProgramBuilder {

  private static AtomicLong nextId = new AtomicLong(1);

  ProgramDefinition.Builder builder;
  AtomicInteger numBlocks = new AtomicInteger(0);

  private ProgramBuilder(ProgramDefinition.Builder builder) {
    this.builder = builder;
  }

  /**
   * Creates {@link ProgramBuilder} with a new {@link Program} with an empty name and description.
   */
  public static ProgramBuilder newProgram() {
    return newProgram("", "");
  }

  /** Creates a {@link ProgramBuilder} with a new {@link Program} with an empty description. */
  public static ProgramBuilder newProgram(String name) {
    return newProgram(name, "");
  }

  /** Creates a {@link ProgramBuilder} with a new {@link Program}. */
  public static ProgramBuilder newProgram(String name, String description) {
    Program program = new Program(name, description);
    maybeSave(program);
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder()
            .setBlockDefinitions(ImmutableList.of())
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setExportDefinitions(ImmutableList.of());
    return new ProgramBuilder(builder);
  }

  private static void maybeSave(Program program) {
    try {
      program.save();
    } catch (ExceptionInInitializerError | NoClassDefFoundError | PersistenceException ignore) {
      program.id = nextId.getAndIncrement();
      program.loadProgramDefinition();
    }
  }

  private static void maybeUpdate(Program program) {
    try {
      program.update();
    } catch (NoClassDefFoundError | PersistenceException ignore) {
      // This is ok not to update if there is no database available.
    }
  }

  public ProgramBuilder withName(String name) {
    builder.addName(Locale.US, name);
    return this;
  }

  public ProgramBuilder withDescription(String description) {
    builder.addDescription(Locale.US, description);
    return this;
  }

  public ProgramBuilder withLifecycleStage(LifecycleStage lifecycleStage) {
    builder.setLifecycleStage(lifecycleStage);
    return this;
  }

  public ProgramBuilder withExportDefinition(ExportDefinition exportDefinition) {
    builder.addExportDefinition(exportDefinition);
    return this;
  }

  /**
   * Creates a {@link BlockBuilder} with this {@link ProgramBuilder} with empty name and
   * description.
   */
  public BlockBuilder withBlock() {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId);
  }

  /** Creates a {@link BlockBuilder} with this {@link ProgramBuilder} with empty description. */
  public BlockBuilder withBlock(String name) {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, name);
  }

  /** Creates a {@link BlockBuilder} with this {@link ProgramBuilder}. */
  public BlockBuilder withBlock(String name, String description) {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, name, description);
  }

  /** Returns the {@link ProgramDefinition} built from this {@link ProgramBuilder}. */
  public ProgramDefinition buildDefinition() {
    Program program = build();
    return program.getProgramDefinition();
  }

  /** Returns the {@link Program} built from this {@link ProgramBuilder}. */
  public Program build() {
    ProgramDefinition programDefinition = builder.build();
    if (programDefinition.blockDefinitions().isEmpty()) {
      return withBlock().build();
    }

    Program program = programDefinition.toProgram();
    maybeUpdate(program);
    return program;
  }

  /**
   * Fluently creates {@link BlockDefinition}s with {@link ProgramBuilder}. This class has no public
   * constructors and should only be used with {@link ProgramBuilder}.
   */
  public static class BlockBuilder {

    private ProgramBuilder programBuilder;
    private BlockDefinition.Builder blockDefBuilder;

    private BlockBuilder(ProgramBuilder programBuilder) {
      this.programBuilder = programBuilder;
      blockDefBuilder = BlockDefinition.builder();
    }

    private static BlockBuilder newBlock(ProgramBuilder programBuilder, long id) {
      BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
      blockBuilder.blockDefBuilder =
          BlockDefinition.builder().setId(id).setName("").setDescription("");
      return blockBuilder;
    }

    private static BlockBuilder newBlock(ProgramBuilder programBuilder, long id, String name) {
      BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
      blockBuilder.blockDefBuilder =
          BlockDefinition.builder().setId(id).setName(name).setDescription("");
      return blockBuilder;
    }

    private static BlockBuilder newBlock(
        ProgramBuilder programBuilder, long id, String name, String description) {
      BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
      blockBuilder.blockDefBuilder =
          BlockDefinition.builder().setId(id).setName(name).setDescription(description);
      return blockBuilder;
    }

    public BlockBuilder withName(String name) {
      blockDefBuilder.setName(name);
      return this;
    }

    public BlockBuilder withDescription(String description) {
      blockDefBuilder.setDescription(description);
      return this;
    }

    public BlockBuilder withHidePredicate(Predicate predicate) {
      blockDefBuilder.setHidePredicate(predicate);
      return this;
    }

    public BlockBuilder withHidePredicate(String predicate) {
      blockDefBuilder.setHidePredicate(Predicate.create(predicate));
      return this;
    }

    public BlockBuilder withOptionalPredicate(Predicate predicate) {
      blockDefBuilder.setOptionalPredicate(predicate);
      return this;
    }

    public BlockBuilder withOptionalPredicate(String predicate) {
      blockDefBuilder.setOptionalPredicate(Predicate.create(predicate));
      return this;
    }

    public BlockBuilder withQuestion(Question question) {
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(question.getQuestionDefinition()));
      return this;
    }

    public BlockBuilder withQuestionDefinition(QuestionDefinition question) {
      blockDefBuilder.addQuestion(ProgramQuestionDefinition.create(question));
      return this;
    }

    public BlockBuilder withQuestions(ImmutableList<Question> questions) {
      ImmutableList<ProgramQuestionDefinition> pqds =
          questions.stream()
              .map(Question::getQuestionDefinition)
              .map(ProgramQuestionDefinition::create)
              .collect(ImmutableList.toImmutableList());
      blockDefBuilder.setProgramQuestionDefinitions(pqds);
      return this;
    }

    public BlockBuilder withQuestionDefinitions(ImmutableList<QuestionDefinition> questions) {
      ImmutableList<ProgramQuestionDefinition> pqds =
          questions.stream()
              .map(ProgramQuestionDefinition::create)
              .collect(ImmutableList.toImmutableList());
      blockDefBuilder.setProgramQuestionDefinitions(pqds);
      return this;
    }
    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder} with an empty name and description.
     */
    public BlockBuilder withBlock() {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      return programBuilder.withBlock();
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder} with an empty description.
     */
    public BlockBuilder withBlock(String name) {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      return programBuilder.withBlock(name);
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder}.
     */
    public BlockBuilder withBlock(String name, String description) {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      return programBuilder.withBlock(name, description);
    }

    /**
     * Returns the {@link ProgramDefinition} built from the {@link ProgramBuilder} with this {@link
     * BlockBuilder}.
     */
    public ProgramDefinition buildDefinition() {
      return build().getProgramDefinition();
    }

    /**
     * Returns the {@link Program} built from the {@link ProgramBuilder} with this {@link
     * BlockBuilder}.
     */
    public Program build() {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      return programBuilder.build();
    }
  }
}
