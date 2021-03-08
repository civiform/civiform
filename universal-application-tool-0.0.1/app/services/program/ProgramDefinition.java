package services.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
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

  /** The list of {@link BlockDefinition}s that make up the program. */
  public abstract ImmutableList<BlockDefinition> blockDefinitions();

  /** The list of {@link ExportDefinition}s that make up the program. */
  public abstract ImmutableList<ExportDefinition> exportDefinitions();

  /** Returns the {@link QuestionDefinition} at the specified block and question indices. */
  @JsonIgnore
  public QuestionDefinition getQuestionDefinition(int blockIndex, int questionIndex) {
    return blockDefinitions().get(blockIndex).getQuestionDefinition(questionIndex);
  }

  /** Returns the {@link BlockDefinition} at the specified block index if available. */
  public Optional<BlockDefinition> getBlockDefinition(int blockIndex) {
    if (blockIndex < 0 || blockIndex >= blockDefinitions().size()) {
      return Optional.empty();
    }
    return Optional.of(blockDefinitions().get(blockIndex));
  }

  /** Returns the {@link BlockDefinition} with the specified block id if available. */
  public Optional<BlockDefinition> getBlockDefinition(long blockId) {
    return blockDefinitions().stream().filter(b -> b.id() == blockId).findAny();
  }

  @JsonIgnore
  public int getBlockCount() {
    return blockDefinitions().size();
  }

  @JsonIgnore
  public int getQuestionCount() {
    return blockDefinitions().stream().mapToInt(BlockDefinition::getQuestionCount).sum();
  }

  /** True if a question with the given question's ID is in the program. */
  @JsonIgnore
  public boolean hasQuestion(QuestionDefinition question) {
    return hasQuestion(question.getId());
  }

  /** True if a question with the given questionId is in the program. */
  @JsonIgnore
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
