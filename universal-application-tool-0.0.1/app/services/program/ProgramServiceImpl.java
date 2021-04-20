package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import forms.BlockForm;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import models.Application;
import models.Program;
import play.db.ebean.Transactional;
import play.libs.concurrent.HttpExecutionContext;
import repository.ProgramRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

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
  public ProgramDefinition getProgramDefinition(long id) throws ProgramNotFoundException {
    try {
      return getProgramDefinitionAsync(id).toCompletableFuture().join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof ProgramNotFoundException) {
        throw new ProgramNotFoundException(id);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletionStage<ProgramDefinition> getProgramDefinitionAsync(long id) {
    return programRepository
        .lookupProgram(id)
        .thenComposeAsync(
            programMaybe ->
                programMaybe.isEmpty()
                    ? CompletableFuture.failedFuture(new ProgramNotFoundException(id))
                    : programMaybe
                        .map(Program::getProgramDefinition)
                        .map(this::syncProgramDefinitionQuestions)
                        .get(),
            httpExecutionContext.current());
  }

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> createProgramDefinition(
      String name, String description) {
    ImmutableSet<CiviFormError> errors = validateProgramDefinition(name, description);
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }
    Program program = new Program(name, description);
    return ErrorAnd.of(programRepository.insertProgramSync(program).getProgramDefinition());
  }

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> updateProgramDefinition(
      long programId, String name, String description) throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    ImmutableSet<CiviFormError> errors = validateProgramDefinition(name, description);
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }
    Program program =
        programDefinition.toBuilder()
            .setLocalizedName(ImmutableMap.of(Locale.US, name))
            .setLocalizedDescription(ImmutableMap.of(Locale.US, description))
            .build()
            .toProgram();
    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  private ImmutableSet<CiviFormError> validateProgramDefinition(String name, String description) {
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.<CiviFormError>builder();
    if (name.isBlank()) {
      errors.add(CiviFormError.of("program name cannot be blank"));
    }
    if (description.isBlank()) {
      errors.add(CiviFormError.of("program description cannot be blank"));
    }
    return errors.build();
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> addBlockToProgram(long programId)
      throws ProgramNotFoundException {
    try {
      return addBlockToProgram(programId, Optional.empty());
    } catch (ProgramBlockNotFoundException e) {
      throw new RuntimeException(
          "The ProgramBlockNotFoundException should never be thrown when the repeater id is"
              + " empty.");
    }
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> addRepeatedBlockToProgram(
      long programId, long repeaterBlockId)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    return addBlockToProgram(programId, Optional.of(repeaterBlockId));
  }

  private ErrorAnd<ProgramDefinition, CiviFormError> addBlockToProgram(
      long programId, Optional<Long> repeaterBlockId)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    if (repeaterBlockId.isPresent() && !programDefinition.hasRepeater(repeaterBlockId.get())) {
      throw new ProgramBlockNotFoundException(programId, repeaterBlockId.get());
    }

    long blockId = getNextBlockId(programDefinition);
    String blockName;
    if (repeaterBlockId.isPresent()) {
      blockName = String.format("Block %d (repeated from %d)", blockId, repeaterBlockId.get());
    } else {
      blockName = String.format("Block %d", blockId);
    }
    String blockDescription =
        "What is the purpose of this block? Add a description that summarizes the information"
            + " collected.";

    ImmutableSet<CiviFormError> errors = validateBlockDefinition(blockName, blockDescription);
    if (!errors.isEmpty()) {
      return ErrorAnd.errorAnd(errors, programDefinition);
    }

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(blockId)
            .setName(blockName)
            .setDescription(blockDescription)
            .setRepeaterId(repeaterBlockId)
            .build();

    Program program =
        programDefinition.toBuilder().addBlockDefinition(blockDefinition).build().toProgram();
    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> updateBlock(
      long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    ImmutableSet<CiviFormError> errors =
        validateBlockDefinition(blockForm.getName(), blockForm.getDescription());
    if (!errors.isEmpty()) {
      return ErrorAnd.errorAnd(errors, programDefinition);
    }

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setName(blockForm.getName())
            .setDescription(blockForm.getDescription())
            .build();

    return ErrorAnd.of(
        updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition));
  }

  private ImmutableSet<CiviFormError> validateBlockDefinition(String name, String description) {
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.<CiviFormError>builder();
    if (name.isBlank()) {
      errors.add(CiviFormError.of("block name cannot be blank"));
    }
    if (description.isBlank()) {
      errors.add(CiviFormError.of("block description cannot be blank"));
    }
    return errors.build();
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockQuestions(
      long programId,
      long blockDefinitionId,
      ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setProgramQuestionDefinitions(programQuestionDefinitions)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition addQuestionsToBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws DuplicateProgramQuestionException, QuestionNotFoundException, ProgramNotFoundException,
          ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    for (long questionId : questionIds) {
      if (programDefinition.hasQuestion(questionId)) {
        throw new DuplicateProgramQuestionException(programId, questionId);
      }
    }

    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

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

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition removeQuestionsFromBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws QuestionNotFoundException, ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    for (long questionId : questionIds) {
      if (!programDefinition.hasQuestion(questionId)) {
        throw new QuestionNotFoundException(questionId, programId);
      }
    }

    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

    ImmutableList<ProgramQuestionDefinition> newProgramQuestionDefinitions =
        blockDefinition.programQuestionDefinitions().stream()
            .filter(pqd -> !questionIds.contains(pqd.id()))
            .collect(ImmutableList.toImmutableList());

    blockDefinition =
        blockDefinition.toBuilder()
            .setProgramQuestionDefinitions(newProgramQuestionDefinitions)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockHidePredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setHidePredicate(Optional.of(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockOptionalPredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setOptionalPredicate(Optional.of(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramNeedsABlockException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    ImmutableList<BlockDefinition> newBlocks =
        programDefinition.blockDefinitions().stream()
            .filter(block -> block.id() != blockDefinitionId)
            .collect(ImmutableList.toImmutableList());
    if (newBlocks.isEmpty()) {
      throw new ProgramNeedsABlockException(programId);
    }

    Program program =
        programDefinition.toBuilder().setBlockDefinitions(newBlocks).build().toProgram();
    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program).getProgramDefinition())
        .toCompletableFuture()
        .join();
  }

  @Override
  public ImmutableList<Application> getProgramApplications(long programId)
      throws ProgramNotFoundException {
    Optional<Program> programMaybe =
        programRepository.lookupProgram(programId).toCompletableFuture().join();
    if (programMaybe.isEmpty()) {
      throw new ProgramNotFoundException(programId);
    }
    return programMaybe.get().getApplications();
  }

  @Override
  public ProgramDefinition newDraftOf(long id) throws ProgramNotFoundException {
    return programRepository
        .createOrUpdateDraft(this.getProgramDefinition(id).toProgram())
        .getProgramDefinition();
  }

  private ProgramDefinition updateProgramDefinitionWithBlockDefinition(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition) {

    ImmutableList<BlockDefinition> updatedBlockDefinitions =
        programDefinition.blockDefinitions().stream()
            .map(b -> b.id() == blockDefinition.id() ? blockDefinition : b)
            .collect(ImmutableList.toImmutableList());

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
    return programDefinition.getMaxBlockDefinitionId() + 1;
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
