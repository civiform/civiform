package support;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import models.DisplayMode;
import models.Program;
import models.Question;
import play.inject.Injector;
import repository.VersionRepository;
import services.program.BlockDefinition;
import services.program.ExportDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.types.QuestionDefinition;

/**
 * The ProgramBuilder can only be used by tests that have a database available because the programs
 * it builds are versioned with the version table, and are persisted in the database.
 *
 * <p>If any tests need to use the ProgramBuilder without a database, new `public static
 * ProgramBuilder newProgram` methods can be added to create programs that are not versioned, and do
 * not get persisted to the database.
 */
public class ProgramBuilder {

  private static Injector injector;

  long programDefinitionId;
  ProgramDefinition.Builder builder;
  AtomicInteger numBlocks = new AtomicInteger(0);

  private ProgramBuilder(long programDefinitionId, ProgramDefinition.Builder builder) {
    this.programDefinitionId = programDefinitionId;
    this.builder = builder;
  }

  public static void setInjector(Injector i) {
    injector = i;
  }

  /**
   * Creates {@link ProgramBuilder} with a new {@link Program} with an empty name and description,
   * in draft state.
   */
  public static ProgramBuilder newDraftProgram() {
    return newDraftProgram("", "");
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link Program} with an empty description, in draft
   * state.
   */
  public static ProgramBuilder newDraftProgram(String name) {
    return newDraftProgram(name, "");
  }

  /** Creates a {@link ProgramBuilder} with a new {@link Program} in draft state. */
  public static ProgramBuilder newDraftProgram(String name, String description) {
    VersionRepository versionRepository = injector.instanceOf(VersionRepository.class);
    Program program =
        new Program(name, description, name, description, "", DisplayMode.PUBLIC.getValue());
    program.addVersion(versionRepository.getDraftVersion());
    program.save();
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder()
            .setBlockDefinitions(ImmutableList.of())
            .setExportDefinitions(ImmutableList.of());
    return new ProgramBuilder(program.id, builder);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link Program} in active state, with blank
   * description and name.
   */
  public static ProgramBuilder newActiveProgram() {
    return newActiveProgram("");
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link Program} in active state, with blank
   * description.
   */
  public static ProgramBuilder newActiveProgram(String name) {
    return newActiveProgram(name, "");
  }

  /** Creates a {@link ProgramBuilder} with a new {@link Program} in active state. */
  public static ProgramBuilder newActiveProgram(String name, String description) {
    VersionRepository versionRepository = injector.instanceOf(VersionRepository.class);
    Program program =
        new Program(name, description, name, description, "", DisplayMode.PUBLIC.getValue());
    program.addVersion(versionRepository.getActiveVersion());
    program.save();
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder()
            .setBlockDefinitions(ImmutableList.of())
            .setExportDefinitions(ImmutableList.of());
    return new ProgramBuilder(program.id, builder);
  }

  public ProgramBuilder withName(String name) {
    builder.setAdminName(name);
    return this;
  }

  public ProgramBuilder withDescription(String description) {
    builder.setAdminDescription(description);
    return this;
  }

  public ProgramBuilder withLocalizedName(Locale locale, String name) {
    builder.addLocalizedName(locale, name);
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
    return BlockBuilder.newBlock(this, blockId, "", "", Optional.empty());
  }

  /** Creates a {@link BlockBuilder} with this {@link ProgramBuilder} with empty description. */
  public BlockBuilder withBlock(String name) {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, name, "", Optional.empty());
  }

  /** Creates a {@link BlockBuilder} with this {@link ProgramBuilder}. */
  public BlockBuilder withBlock(String name, String description) {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, name, description, Optional.empty());
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
    program.update();
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

    private static BlockBuilder newBlock(
        ProgramBuilder programBuilder,
        long id,
        String name,
        String description,
        Optional<Long> enumeratorId) {
      BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
      blockBuilder.blockDefBuilder =
          BlockDefinition.builder()
              .setId(id)
              .setName(name)
              .setDescription(description)
              .setEnumeratorId(enumeratorId);
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

    public BlockBuilder withPredicate(PredicateDefinition predicate) {
      blockDefBuilder.setVisibilityPredicate(predicate);
      return this;
    }

    /** Add a required question to the block. */
    public BlockBuilder withRequiredQuestion(Question question) {
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
              question.getQuestionDefinition(), Optional.of(programBuilder.programDefinitionId)));
      return this;
    }

    public BlockBuilder withOptionalQuestion(Question question) {
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
                  question.getQuestionDefinition(), Optional.of(programBuilder.programDefinitionId))
              .setOptional(true));
      return this;
    }

    /** Add a required question definition to the block. */
    public BlockBuilder withRequiredQuestionDefinition(QuestionDefinition question) {
      return withQuestionDefinition(question, false);
    }

    public BlockBuilder withQuestionDefinition(QuestionDefinition question, boolean optional) {
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
                  question, Optional.of(programBuilder.programDefinitionId))
              .setOptional(optional));
      return this;
    }

    public BlockBuilder withRequiredQuestions(Question... questions) {
      return withRequiredQuestions(ImmutableList.copyOf(questions));
    }

    public BlockBuilder withRequiredQuestions(ImmutableList<Question> questions) {
      ImmutableList<ProgramQuestionDefinition> pqds =
          questions.stream()
              .map(Question::getQuestionDefinition)
              .map(
                  questionDefinition ->
                      ProgramQuestionDefinition.create(
                          questionDefinition, Optional.of(programBuilder.programDefinitionId)))
              .collect(ImmutableList.toImmutableList());
      blockDefBuilder.setProgramQuestionDefinitions(pqds);
      return this;
    }

    public BlockBuilder withRequiredQuestionDefinitions(
        ImmutableList<QuestionDefinition> questions) {
      ImmutableList<ProgramQuestionDefinition> pqds =
          questions.stream()
              .map(
                  questionDefinition ->
                      ProgramQuestionDefinition.create(
                          questionDefinition, Optional.of(programBuilder.programDefinitionId)))
              .collect(ImmutableList.toImmutableList());
      blockDefBuilder.setProgramQuestionDefinitions(pqds);
      return this;
    }
    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder} with an empty name and description.
     */
    public BlockBuilder withBlock() {
      return withBlock("", "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder} with an empty description.
     */
    public BlockBuilder withBlock(String name) {
      return withBlock(name, "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder}.
     */
    public BlockBuilder withBlock(String name, String description) {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      long blockId = Long.valueOf(programBuilder.numBlocks.incrementAndGet());
      return BlockBuilder.newBlock(programBuilder, blockId, name, description, Optional.empty());
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that has this block as its
     * enumerator, with an empty name and description.
     */
    public BlockBuilder withRepeatedBlock() {
      return withRepeatedBlock("", "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that has this block as its
     * enumerator, with an empty description.
     */
    public BlockBuilder withRepeatedBlock(String name) {
      return withRepeatedBlock(name, "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that has this block as its
     * enumerator.
     */
    public BlockBuilder withRepeatedBlock(String name, String description) {
      BlockDefinition thisBlock = blockDefBuilder.build();
      if (!thisBlock.isEnumerator()) {
        throw new RuntimeException(
            "Cannot create a repeated block if this block is not an enumerator.");
      }
      programBuilder.builder.addBlockDefinition(thisBlock);
      long blockId = Long.valueOf(programBuilder.numBlocks.incrementAndGet());
      return BlockBuilder.newBlock(
          programBuilder, blockId, name, description, Optional.of(thisBlock.id()));
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that shares an enumerator
     * block with this block, with an empty name and description.
     */
    public BlockBuilder withAnotherRepeatedBlock() {
      return withAnotherRepeatedBlock("", "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that shares an enumerator
     * block with this block, with an empty description.
     */
    public BlockBuilder withAnotherRepeatedBlock(String name) {
      return withAnotherRepeatedBlock(name, "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that shares an enumerator
     * block with this block.
     */
    public BlockBuilder withAnotherRepeatedBlock(String name, String description) {
      BlockDefinition thisBlock = blockDefBuilder.build();
      if (!thisBlock.isRepeated()) {
        throw new RuntimeException(
            "Cannot create a another repeated block if this block is not repeated.");
      }
      programBuilder.builder.addBlockDefinition(thisBlock);
      long blockId = Long.valueOf(programBuilder.numBlocks.incrementAndGet());
      return BlockBuilder.newBlock(
          programBuilder, blockId, name, description, thisBlock.enumeratorId());
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
