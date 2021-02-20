package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Program;
import play.db.ebean.Transactional;
import repository.ProgramRepository;
import services.question.QuestionDefinition;

public class ProgramServiceImpl implements ProgramService {

  private ProgramRepository programRepository;

  @Inject
  public ProgramServiceImpl(ProgramRepository programRepository) {
    this.programRepository = checkNotNull(programRepository);
  }

  @Override
  public ImmutableList<ProgramDefinition> listProgramDefinitions() {
    return listProgramDefinitionsAsync().toCompletableFuture().join();
  }

  @Override
  public CompletionStage<ImmutableList<ProgramDefinition>> listProgramDefinitionsAsync() {
    return programRepository
        .listPrograms()
        .thenApply(
            programs ->
                programs.stream()
                    .map(program -> program.getProgramDefinition())
                    .collect(ImmutableList.toImmutableList()));
  }

  @Override
  public Optional<ProgramDefinition> getProgramDefinition(long id) {
    return getProgramDefinitionAsync(id).toCompletableFuture().join();
  }

  @Override
  public CompletionStage<Optional<ProgramDefinition>> getProgramDefinitionAsync(long id) {
    return programRepository
        .lookupProgram(id)
        .thenApply(optionalProgram -> optionalProgram.map(Program::getProgramDefinition));
  }

  @Override
  public ProgramDefinition createProgramDefinition(String name, String description) {
    Program program = new Program(name, description);
    return programRepository.insertProgramSync(program).getProgramDefinition();
  }

  @Override
  public ProgramDefinition updateProgramDefinition(long programId, String name, String description)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    Program program =
        programDefinition.toBuilder().setName(name).setDescription(description).build().toProgram();
    return programRepository.updateProgramSync(program).getProgramDefinition();
  }

  @Override
  @Transactional
  public ProgramDefinition addBlockToProgram(
      long programId, String blockName, String blockDescription) throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    long blockId = getNextBlockId(programDefinition);

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(blockId)
            .setName(blockName)
            .setDescription(blockDescription)
            .build();

    Program program =
        programDefinition.toBuilder().addBlockDefinition(blockDefinition).build().toProgram();
    return programRepository.updateProgramSync(program).getProgramDefinition();
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockQuestions(
      long programId, long blockDefinitionId, ImmutableList<QuestionDefinition> questionDefinitions)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex).toBuilder()
            .setQuestionDefinitions(questionDefinitions)
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programDefinition, blockDefinitionIndex, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockHidePredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex).toBuilder()
            .setHidePredicate(Optional.of(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programDefinition, blockDefinitionIndex, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockOptionalPredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex).toBuilder()
            .setOptionalPredicate(Optional.of(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programDefinition, blockDefinitionIndex, blockDefinition);
  }

  private ProgramDefinition getProgramOrThrow(long programId) throws ProgramNotFoundException {
    return getProgramDefinition(programId)
        .orElseThrow(() -> new ProgramNotFoundException((programId)));
  }

  private int getBlockDefinitionIndex(ProgramDefinition programDefinition, Long blockDefinitionId)
      throws ProgramBlockNotFoundException {
    int index =
        programDefinition.blockDefinitions().stream()
            .map(b -> b.id())
            .collect(ImmutableList.toImmutableList())
            .indexOf(blockDefinitionId);

    if (index == -1) {
      throw new ProgramBlockNotFoundException(programDefinition.id(), blockDefinitionId);
    }

    return index;
  }

  private ProgramDefinition updateProgramDefinitionWithBlockDefinition(
      ProgramDefinition programDefinition,
      int blockDefinitionIndex,
      BlockDefinition blockDefinition) {

    List<BlockDefinition> mutableBlockDefinitions =
        new ArrayList<>(programDefinition.blockDefinitions());
    mutableBlockDefinitions.set(blockDefinitionIndex, blockDefinition);
    ImmutableList<BlockDefinition> updatedBlockDefinitions =
        ImmutableList.copyOf(mutableBlockDefinitions);

    Program program =
        programDefinition.toBuilder()
            .setBlockDefinitions(updatedBlockDefinitions)
            .build()
            .toProgram();
    return programRepository.updateProgramSync(program).getProgramDefinition();
  }

  private long getNextBlockId(ProgramDefinition programDefinition) {
    return programDefinition.blockDefinitions().stream()
            .map(BlockDefinition::id)
            .max(Long::compareTo)
            .orElseGet(() -> 0L)
        + 1;
  }
}
