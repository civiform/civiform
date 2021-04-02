package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import models.LifecycleStage;
import models.Program;
import services.question.QuestionDefinition;

@AutoValue
public abstract class ProgramDefinition {

  private Optional<ImmutableSet<Long>> questionIds = Optional.empty();

  public static Builder builder() {
    return new AutoValue_ProgramDefinition.Builder();
  }

  /** Unique identifier for a ProgramDefinition. */
  public abstract long id();

  /** Descriptive name of a Program, e.g. Car Tab Rebate Program */
  public abstract String name();

  /** A human readable description of a Program. */
  public abstract String description();

  /** The lifecycle stage of the Program. */
  public abstract LifecycleStage lifecycleStage();

  /** The list of {@link BlockDefinition}s that make up the program. */
  public abstract ImmutableList<BlockDefinition> blockDefinitions();

  /** The list of {@link ExportDefinition}s that make up the program. */
  public abstract ImmutableList<ExportDefinition> exportDefinitions();

  /** Returns the {@link QuestionDefinition} at the specified block and question indices. */
  public QuestionDefinition getQuestionDefinition(int blockIndex, int questionIndex) {
    return blockDefinitions().get(blockIndex).getQuestionDefinition(questionIndex);
  }

  /** Returns the {@link BlockDefinition} at the specified block index if available. */
  public Optional<BlockDefinition> getBlockDefinitionByIndex(int blockIndex) {
    if (blockIndex < 0 || blockIndex >= blockDefinitions().size()) {
      return Optional.empty();
    }
    return Optional.of(blockDefinitions().get(blockIndex));
  }

  /**
   * Get the {@link BlockDefinition} with the specified block definition id.
   *
   * @param blockDefinitionId the id of the block definition
   * @return the {@link BlockDefinition} with the specified block id
   * @throws ProgramBlockNotFoundException if no block matched the block id
   */
  public BlockDefinition getBlockDefinition(long blockDefinitionId) throws ProgramBlockNotFoundException {
    return blockDefinitions().stream()
        .filter(b -> b.id() == blockDefinitionId)
        .findAny()
        .orElseThrow(() -> new ProgramBlockNotFoundException(id(), blockDefinitionId));
  }

  public BlockDefinition getBlockDefinition(String blockId) throws ProgramBlockNotFoundException {
    // TODO: add a new exception for malformed blockId.
    // TODO: refactor this blockId parsing to a shared method somewhere with appropriate context.
    long blockDefinitionId = Splitter.on("-").splitToStream(blockId).map(Long::valueOf).findFirst().orElseThrow();
    return getBlockDefinition(blockDefinitionId);
  }

  /**
   * Get the last {@link BlockDefinition} of the program.
   *
   * @return the last {@link BlockDefinition}
   * @throws ProgramNeedsABlockException if the program has no blocks
   */
  public BlockDefinition getLastBlockDefinition() throws ProgramNeedsABlockException {
    return getBlockDefinitionByIndex(blockDefinitions().size() - 1)
        .orElseThrow(() -> new ProgramNeedsABlockException(id()));
  }

  /** Returns the max block definition id. */
  public long getMaxBlockDefinitionId() {
    return blockDefinitions().stream()
        .map(BlockDefinition::id)
        .max(Long::compareTo)
        .orElseGet(() -> 0L);
  }

  public int getBlockCount() {
    return blockDefinitions().size();
  }

  public int getQuestionCount() {
    return blockDefinitions().stream().mapToInt(BlockDefinition::getQuestionCount).sum();
  }

  /** True if a question with the given question's ID is in the program. */
  public boolean hasQuestion(QuestionDefinition question) {
    return hasQuestion(question.getId());
  }

  /** True if a question with the given questionId is in the program. */
  public boolean hasQuestion(long questionId) {
    if (questionIds.isEmpty()) {
      questionIds =
          Optional.of(
              blockDefinitions().stream()
                  .map(BlockDefinition::programQuestionDefinitions)
                  .flatMap(ImmutableList::stream)
                  .map(ProgramQuestionDefinition::getQuestionDefinition)
                  .map(QuestionDefinition::getId)
                  .collect(ImmutableSet.toImmutableSet()));
    }

    return questionIds.get().contains(questionId);
  }

  public Program toProgram() {
    return new Program(this);
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(long id);

    public abstract Builder setName(String name);

    public abstract Builder setDescription(String description);

    public abstract Builder setBlockDefinitions(ImmutableList<BlockDefinition> blockDefinitions);

    public abstract Builder setExportDefinitions(ImmutableList<ExportDefinition> exportDefinitions);

    public abstract Builder setLifecycleStage(LifecycleStage lifecycleStage);

    public abstract ImmutableList.Builder<BlockDefinition> blockDefinitionsBuilder();

    public abstract ImmutableList.Builder<ExportDefinition> exportDefinitionsBuilder();

    public abstract ProgramDefinition build();

    public Builder addBlockDefinition(BlockDefinition blockDefinition) {
      blockDefinitionsBuilder().add(blockDefinition);
      return this;
    }

    public Builder addExportDefinition(ExportDefinition exportDefinition) {
      exportDefinitionsBuilder().add(exportDefinition);
      return this;
    }
  }
}
