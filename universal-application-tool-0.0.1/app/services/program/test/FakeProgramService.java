package services.program.test;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import services.program.*;
import services.question.QuestionDefinition;

/**
 * A fake implementation of {@link ProgramService} that uses an in-memory map instead of a database.
 */
public class FakeProgramService implements ProgramService {

  private final Map<Long, ProgramDefinition> programs = new HashMap<>();
  private long nextId = 1L;

  @Override
  public ImmutableList<ProgramDefinition> listProgramDefinitions() {
    return ImmutableList.copyOf(this.programs.values());
  }

  @Override
  public CompletionStage<ImmutableList<ProgramDefinition>> listProgramDefinitionsAsync() {
    return CompletableFuture.completedFuture(listProgramDefinitions());
  }

  @Override
  public Optional<ProgramDefinition> getProgramDefinition(long id) {
    return Optional.ofNullable(this.programs.get(id));
  }

  @Override
  public CompletionStage<Optional<ProgramDefinition>> getProgramDefinitionAsync(long id) {
    return CompletableFuture.completedFuture(getProgramDefinition(id));
  }

  @Override
  public ProgramDefinition createProgramDefinition(String name, String description) {
    ProgramDefinition programDefinition =
        ProgramDefinition.builder().setId(nextId).setName(name).setDescription(description).build();
    this.programs.put(nextId, programDefinition);
    this.nextId += 1;
    return programDefinition;
  }

  @Override
  public ProgramDefinition updateProgramDefinition(long programId, String name, String description)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    ProgramDefinition updated =
        programDefinition.toBuilder().setName(name).setDescription(description).build();
    this.programs.put(programId, updated);
    return updated;
  }

  @Override
  public ProgramDefinition addBlockToProgram(
      long programId, String blockName, String blockDescription) throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(getNextBlockId(programDefinition))
            .setName(blockName)
            .setDescription(blockDescription)
            .build();
    programDefinition = programDefinition.toBuilder().addBlockDefinition(blockDefinition).build();
    this.programs.put(programId, programDefinition);

    return programDefinition;
  }

  @Override
  public ProgramDefinition setBlockQuestions(
      long programId, long blockDefinitionId, ImmutableList<QuestionDefinition> questionDefinitions)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programToUpdate = getProgramOrThrow(programId);
    int blockIndex = getIndexOfBlock(programToUpdate, blockDefinitionId);

    BlockDefinition updatedBlockDefinition =
        programToUpdate.blockDefinitions().get(blockIndex).toBuilder()
            .setQuestionDefinitions(questionDefinitions)
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programToUpdate, blockIndex, updatedBlockDefinition);
  }

  @Override
  public ProgramDefinition setBlockHidePredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programToUpdate = getProgramOrThrow(programId);
    int blockIndex = getIndexOfBlock(programToUpdate, blockDefinitionId);

    BlockDefinition updatedBlockDefinition =
        programToUpdate.blockDefinitions().get(blockIndex).toBuilder()
            .setHidePredicate(Optional.of(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programToUpdate, blockIndex, updatedBlockDefinition);
  }

  @Override
  public ProgramDefinition setBlockOptionalPredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programToUpdate = getProgramOrThrow(programId);
    int blockIndex = getIndexOfBlock(programToUpdate, blockDefinitionId);

    BlockDefinition updatedBlockDefinition =
        programToUpdate.blockDefinitions().get(blockIndex).toBuilder()
            .setOptionalPredicate(Optional.of(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programToUpdate, blockIndex, updatedBlockDefinition);
  }

  private ProgramDefinition getProgramOrThrow(long programId) throws ProgramNotFoundException {
    if (this.programs.containsKey(programId)) {
      return this.programs.get(programId);
    } else {
      throw new ProgramNotFoundException(programId);
    }
  }

  private int getIndexOfBlock(ProgramDefinition program, long blockId)
      throws ProgramBlockNotFoundException {
    Optional<BlockDefinition> blockDefinition =
        program.blockDefinitions().stream().filter(b -> b.id() == blockId).findFirst();
    if (blockDefinition.isPresent()) {
      return program.blockDefinitions().indexOf(blockDefinition.get());
    } else {
      throw new ProgramBlockNotFoundException(program.id(), blockId);
    }
  }

  private long getNextBlockId(ProgramDefinition programDefinition) {
    return programDefinition.blockDefinitions().stream()
            .map(BlockDefinition::id)
            .max(Long::compareTo)
            .orElseGet(() -> 0L)
        + 1;
  }

  private ProgramDefinition updateProgramDefinitionWithBlockDefinition(
      ProgramDefinition programDefinition,
      int blockDefinitionIndex,
      BlockDefinition blockDefinition) {
    List<BlockDefinition> mutableBlockDefinitions =
        new ArrayList<>(programDefinition.blockDefinitions());
    mutableBlockDefinitions.set(blockDefinitionIndex, blockDefinition);

    programDefinition =
        programDefinition.toBuilder()
            .setBlockDefinitions(ImmutableList.copyOf(mutableBlockDefinitions))
            .build();
    this.programs.put(programDefinition.id(), programDefinition);

    return programDefinition;
  }
}
