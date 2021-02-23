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
import services.question.QuestionNotFoundException;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;

public class ProgramServiceImpl implements ProgramService {

  private ProgramRepository programRepository;
  private QuestionService questionService;

  @Inject
  public ProgramServiceImpl(ProgramRepository programRepository, QuestionService questionService) {
    this.programRepository = checkNotNull(programRepository);
    this.questionService = checkNotNull(questionService);
  }

  @Override
  public ImmutableList<ProgramDefinition> listProgramDefinitions() {
    return listProgramDefinitionsAsync().toCompletableFuture().join();
  }

  @Override
  public CompletionStage<ImmutableList<ProgramDefinition>> listProgramDefinitionsAsync() {
    return programRepository
        .listPrograms()
        .thenComposeAsync(
            programs ->
                questionService
                    .getReadOnlyQuestionService()
                    .thenApply(
                        roQuestionService ->
                            programs.stream()
                                .map(
                                    program ->
                                        syncQuestions(
                                            program.getProgramDefinition(), roQuestionService))
                                .collect(ImmutableList.toImmutableList())));
  }

  @Override
  public Optional<ProgramDefinition> getProgramDefinition(long id) {
    return getProgramDefinitionAsync(id).toCompletableFuture().join();
  }

  @Override
  public CompletionStage<Optional<ProgramDefinition>> getProgramDefinitionAsync(long id) {
    return programRepository
        .lookupProgram(id)
        .thenApply(
            optionalProgram -> optionalProgram.map((p) -> syncQuestions(p.getProgramDefinition())));
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
    return syncQuestions(programRepository.updateProgramSync(program).getProgramDefinition());
  }

  @Override
  @Transactional
  public ProgramDefinition addBlockToProgram(
      long programId, String blockName, String blockDescription) throws ProgramNotFoundException {
    return addBlockToProgram(programId, blockName, blockDescription, ImmutableList.of());
  }

  @Override
  @Transactional
  public ProgramDefinition addBlockToProgram(
      long programId,
      String blockName,
      String blockDescription,
      ImmutableList<ProgramQuestionDefinition> questionDefinitions)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    long blockId = getNextBlockId(programDefinition);

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(blockId)
            .setName(blockName)
            .setDescription(blockDescription)
            .setProgramQuestionDefinitions(questionDefinitions)
            .build();

    Program program =
        programDefinition.toBuilder().addBlockDefinition(blockDefinition).build().toProgram();
    return syncQuestions(programRepository.updateProgramSync(program).getProgramDefinition());
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockQuestions(
      long programId,
      long blockDefinitionId,
      ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex).toBuilder()
            .setProgramQuestionDefinitions(programQuestionDefinitions)
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
    return syncQuestions(programRepository.updateProgramSync(program).getProgramDefinition());
  }

  private long getNextBlockId(ProgramDefinition programDefinition) {
    return programDefinition.blockDefinitions().stream()
            .map(BlockDefinition::id)
            .max(Long::compareTo)
            .orElseGet(() -> 0L)
        + 1;
  }

  /**
   * Update all {@link QuestionDefinition}s in the ProgramDefinition with appropriate versions from
   * the {@link QuestionService}.
   */
  private ProgramDefinition syncQuestions(ProgramDefinition programDefinition) {
    return questionService
        .getReadOnlyQuestionService()
        .thenApply(
            (roQuestionService) -> {
              return syncQuestions(programDefinition, roQuestionService);
            })
        .toCompletableFuture()
        .join();
  }

  private ProgramDefinition syncQuestions(
      ProgramDefinition programDefinition, ReadOnlyQuestionService roQuestionService) {
    ProgramDefinition.Builder programDefinitionBuilder = programDefinition.toBuilder();

    ImmutableList.Builder<BlockDefinition> blockListBuilder = ImmutableList.builder();
    for (BlockDefinition block : programDefinition.blockDefinitions()) {
      BlockDefinition.Builder blockBuilder = block.toBuilder();

      ImmutableList.Builder<ProgramQuestionDefinition> pqdListBuilder = ImmutableList.builder();
      for (ProgramQuestionDefinition pqd : block.programQuestionDefinitions()) {
        QuestionDefinition questionDefinition;
        try {
          questionDefinition = roQuestionService.getQuestionDefinition(pqd.id());
        } catch (QuestionNotFoundException e) {
          throw new RuntimeException(e);
        }
        pqdListBuilder.add(ProgramQuestionDefinition.create(questionDefinition));
      }

      blockBuilder.setProgramQuestionDefinitions(pqdListBuilder.build());
      blockListBuilder.add(blockBuilder.build());
    }

    programDefinitionBuilder.setBlockDefinitions(blockListBuilder.build());
    return programDefinitionBuilder.build();
  }
}
