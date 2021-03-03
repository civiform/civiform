package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.BlockForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import models.Program;
import play.db.ebean.Transactional;
import play.libs.concurrent.HttpExecutionContext;
import repository.ProgramRepository;
import services.question.QuestionDefinition;
import services.question.QuestionNotFoundException;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;

public class ProgramServiceImpl implements ProgramService {

  private final ProgramRepository programRepository;
  private final QuestionService questionService;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ProgramServiceImpl(
      ProgramRepository programRepository,
      QuestionService questionService,
      HttpExecutionContext ec) {
    this.programRepository = checkNotNull(programRepository);
    this.questionService = checkNotNull(questionService);
    this.httpExecutionContext = checkNotNull(ec);
  }

  @Override
  public ImmutableList<ProgramDefinition> listProgramDefinitions() {
    return listProgramDefinitionsAsync().toCompletableFuture().join();
  }

  @Override
  public CompletionStage<ImmutableList<ProgramDefinition>> listProgramDefinitionsAsync() {
    CompletableFuture<ReadOnlyQuestionService> roQuestionServiceFuture =
        questionService.getReadOnlyQuestionService().toCompletableFuture();
    CompletableFuture<ImmutableList<Program>> programsFuture =
        programRepository.listPrograms().toCompletableFuture();

    return CompletableFuture.allOf(roQuestionServiceFuture, programsFuture)
        .thenApplyAsync(
            ignore -> {
              ReadOnlyQuestionService roQuestionService = roQuestionServiceFuture.join();
              ImmutableList<Program> programs = programsFuture.join();

              return programs.stream()
                  .map(
                      program ->
                          syncProgramDefinitionQuestions(
                              program.getProgramDefinition(), roQuestionService))
                  .collect(ImmutableList.toImmutableList());
            },
            httpExecutionContext.current());
  }

  @Override
  public Optional<ProgramDefinition> getProgramDefinition(long id) {
    return getProgramDefinitionAsync(id).toCompletableFuture().join();
  }

  @Override
  public CompletionStage<Optional<ProgramDefinition>> getProgramDefinitionAsync(long id) {
    return programRepository
        .lookupProgram(id)
        .thenComposeAsync(
            programMaybe ->
                programMaybe.isEmpty()
                    ? CompletableFuture.completedFuture(Optional.empty())
                    : programMaybe
                        .map(Program::getProgramDefinition)
                        .map(this::syncProgramDefinitionQuestions)
                        .get()
                        .thenApply(Optional::of),
            httpExecutionContext.current());
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
    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program).getProgramDefinition())
        .toCompletableFuture()
        .join();
  }

  @Override
  @Transactional
  public ProgramDefinition addBlockToProgram(long programId) throws ProgramNotFoundException {
    ProgramDefinition program = getProgramOrThrow(programId);
    String blockName = String.format("Block %d", program.blockDefinitions().size() + 1);

    return addBlockToProgram(programId, blockName, "", ImmutableList.of());
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
    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program).getProgramDefinition())
        .toCompletableFuture()
        .join();
  }

  @Override
  @Transactional
  public ProgramDefinition updateBlock(long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);
    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex).toBuilder()
            .setName(blockForm.getName())
            .setDescription(blockForm.getDescription())
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programDefinition, blockDefinitionIndex, blockDefinition);
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
  public ProgramDefinition addQuestionsToBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws DuplicateProgramQuestionException, QuestionNotFoundException, ProgramNotFoundException,
          ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);

    for (long questionId : questionIds) {
      if (programDefinition.hasQuestion(questionId)) {
        throw new DuplicateProgramQuestionException(programId, questionId);
      }
    }

    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex);

    ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions =
        blockDefinition.programQuestionDefinitions();

    ImmutableList.Builder<ProgramQuestionDefinition> newQuestionListBuilder =
        ImmutableList.builder();
    newQuestionListBuilder.addAll(programQuestionDefinitions);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    for (long qid : questionIds) {
      newQuestionListBuilder.add(
          ProgramQuestionDefinition.create(roQuestionService.getQuestionDefinition(qid)));
    }

    ImmutableList<ProgramQuestionDefinition> newProgramQuestionDefinitions =
        newQuestionListBuilder.build();

    blockDefinition =
        blockDefinition.toBuilder()
            .setProgramQuestionDefinitions(newProgramQuestionDefinitions)
            .build();

    return updateProgramDefinitionWithBlockDefinition(
        programDefinition, blockDefinitionIndex, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition removeQuestionsFromBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws QuestionNotFoundException, ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);

    for (long questionId : questionIds) {
      if (!programDefinition.hasQuestion(questionId)) {
        throw new QuestionNotFoundException(questionId, programId);
      }
    }

    int blockDefinitionIndex = getBlockDefinitionIndex(programDefinition, blockDefinitionId);

    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().get(blockDefinitionIndex);

    ImmutableList<ProgramQuestionDefinition> newProgramQuestionDefinitions =
        blockDefinition.programQuestionDefinitions().stream()
            .filter(pqd -> !questionIds.contains(pqd.id()))
            .collect(ImmutableList.toImmutableList());

    blockDefinition =
        blockDefinition.toBuilder()
            .setProgramQuestionDefinitions(newProgramQuestionDefinitions)
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

  @Override
  @Transactional
  public ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramOrThrow(programId);

    ImmutableList<BlockDefinition> newBlocks =
        programDefinition.blockDefinitions().stream()
            .filter(block -> block.id() != blockDefinitionId)
            .collect(ImmutableList.toImmutableList());

    Program program =
        programDefinition.toBuilder().setBlockDefinitions(newBlocks).build().toProgram();
    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program).getProgramDefinition())
        .toCompletableFuture()
        .join();
  }

  private ProgramDefinition getProgramOrThrow(long programId) throws ProgramNotFoundException {
    return getProgramDefinition(programId)
        .orElseThrow(() -> new ProgramNotFoundException(programId));
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
    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program).getProgramDefinition())
        .toCompletableFuture()
        .join();
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
  private CompletionStage<ProgramDefinition> syncProgramDefinitionQuestions(
      ProgramDefinition programDefinition) {
    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            roQuestionService ->
                syncProgramDefinitionQuestions(programDefinition, roQuestionService),
            httpExecutionContext.current());
  }

  private ProgramDefinition syncProgramDefinitionQuestions(
      ProgramDefinition programDefinition, ReadOnlyQuestionService roQuestionService) {
    ProgramDefinition.Builder programDefinitionBuilder = programDefinition.toBuilder();

    ImmutableList.Builder<BlockDefinition> blockListBuilder = ImmutableList.builder();
    for (BlockDefinition block : programDefinition.blockDefinitions()) {
      BlockDefinition syncedBlock = syncBlockDefinitionQuestions(block, roQuestionService);
      blockListBuilder.add(syncedBlock);
    }

    programDefinitionBuilder.setBlockDefinitions(blockListBuilder.build());
    return programDefinitionBuilder.build();
  }

  private BlockDefinition syncBlockDefinitionQuestions(
      BlockDefinition blockDefinition, ReadOnlyQuestionService roQuestionService) {
    BlockDefinition.Builder blockBuilder = blockDefinition.toBuilder();

    ImmutableList.Builder<ProgramQuestionDefinition> pqdListBuilder = ImmutableList.builder();
    for (ProgramQuestionDefinition pqd : blockDefinition.programQuestionDefinitions()) {
      ProgramQuestionDefinition syncedPqd = syncProgramQuestionDefinition(pqd, roQuestionService);
      pqdListBuilder.add(syncedPqd);
    }

    blockBuilder.setProgramQuestionDefinitions(pqdListBuilder.build());
    return blockBuilder.build();
  }

  private ProgramQuestionDefinition syncProgramQuestionDefinition(
      ProgramQuestionDefinition pqd, ReadOnlyQuestionService roQuestionService) {
    QuestionDefinition questionDefinition;
    try {
      questionDefinition = roQuestionService.getQuestionDefinition(pqd.id());
    } catch (QuestionNotFoundException e) {
      throw new RuntimeException(e);
    }
    return ProgramQuestionDefinition.create(questionDefinition);
  }
}
