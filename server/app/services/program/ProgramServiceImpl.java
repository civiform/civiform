package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.slugify.Slugify;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import forms.BlockForm;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;
import models.Account;
import models.Application;
import models.DisplayMode;
import models.Program;
import models.Version;
import play.db.ebean.Transactional;
import play.libs.concurrent.HttpExecutionContext;
import repository.ProgramRepository;
import repository.UserRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.PaginationResult;
import services.PaginationSpec;
import services.program.predicate.PredicateDefinition;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

/** Implementation class for {@link ProgramService} interface. */
public class ProgramServiceImpl implements ProgramService {

  private final Slugify slugifier = new Slugify();
  private final ProgramRepository programRepository;
  private final QuestionService questionService;
  private final HttpExecutionContext httpExecutionContext;
  private final UserRepository userRepository;
  private final VersionRepository versionRepository;

  @Inject
  public ProgramServiceImpl(
      ProgramRepository programRepository,
      QuestionService questionService,
      UserRepository userRepository,
      VersionRepository versionRepository,
      HttpExecutionContext ec) {
    this.programRepository = checkNotNull(programRepository);
    this.questionService = checkNotNull(questionService);
    this.httpExecutionContext = checkNotNull(ec);
    this.userRepository = checkNotNull(userRepository);
    this.versionRepository = checkNotNull(versionRepository);
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
  public ActiveAndDraftPrograms getActiveAndDraftPrograms() {
    return new ActiveAndDraftPrograms(
        this, versionRepository.getActiveVersion(), versionRepository.getDraftVersion());
  }

  @Override
  public CompletionStage<ProgramDefinition> getProgramDefinitionAsync(long id) {
    return programRepository
        .lookupProgram(id)
        .thenComposeAsync(
            programMaybe -> {
              if (programMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(new ProgramNotFoundException(id));
              }

              return syncProgramAssociations(programMaybe.get());
            },
            httpExecutionContext.current());
  }

  private CompletionStage<ProgramDefinition> syncProgramAssociations(Program program) {
    if (isActiveOrDraftProgram(program)) {
      return syncProgramDefinitionQuestions(program.getProgramDefinition())
          .thenApply(programDefinition -> programDefinition.orderBlockDefinitions());
    }

    // Any version that the program is in has all the questions the program has.
    Version version = program.getVersions().stream().findAny().get();
    ProgramDefinition programDefinition =
        syncProgramDefinitionQuestions(program.getProgramDefinition(), version);

    return CompletableFuture.completedStage(programDefinition.orderBlockDefinitions());
  }

  @Override
  public ImmutableSet<String> getActiveProgramNames() {
    return versionRepository.getActiveVersion().getProgramsNames();
  }

  @Override
  public ImmutableSet<String> getAllProgramNames() {
    return programRepository.getAllProgramNames();
  }

  @Override
  public ImmutableSet<String> getAllProgramSlugs() {
    return getAllProgramNames().stream()
        .map(slugifier::slugify)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> createProgramDefinition(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription,
      String externalLink,
      String displayMode) {

    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();

    if (hasProgramNameCollision(adminName)) {
      errorsBuilder.add(CiviFormError.of("a program named " + adminName + " already exists"));
    }

    validateProgramText(errorsBuilder, "admin name", adminName);
    validateProgramText(errorsBuilder, "admin description", adminDescription);
    validateProgramText(errorsBuilder, "display name", defaultDisplayName);
    validateProgramText(errorsBuilder, "display description", defaultDisplayDescription);
    validateProgramText(errorsBuilder, "display mode", displayMode);

    ImmutableSet<CiviFormError> errors = errorsBuilder.build();

    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    Program program =
        new Program(
            adminName,
            adminDescription,
            defaultDisplayName,
            defaultDisplayDescription,
            externalLink,
            displayMode);

    program.addVersion(versionRepository.getDraftVersion());
    return ErrorAnd.of(programRepository.insertProgramSync(program).getProgramDefinition());
  }

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> updateProgramDefinition(
      long programId,
      Locale locale,
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();
    validateProgramText(errorsBuilder, "admin description", adminDescription);
    validateProgramText(errorsBuilder, "display name", displayName);
    validateProgramText(errorsBuilder, "display description", displayDescription);
    ImmutableSet<CiviFormError> errors = errorsBuilder.build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    Program program =
        programDefinition.toBuilder()
            .setAdminDescription(adminDescription)
            .setLocalizedName(
                programDefinition.localizedName().updateTranslation(locale, displayName))
            .setLocalizedDescription(
                programDefinition
                    .localizedDescription()
                    .updateTranslation(locale, displayDescription))
            .setExternalLink(externalLink)
            .setDisplayMode(DisplayMode.valueOf(displayMode))
            .build()
            .toProgram();
    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> updateLocalization(
      long programId, Locale locale, String displayName, String displayDescription)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();
    validateProgramText(errorsBuilder, "display name", displayName);
    validateProgramText(errorsBuilder, "display description", displayDescription);
    ImmutableSet<CiviFormError> errors = errorsBuilder.build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    Program program =
        programDefinition.toBuilder()
            .setLocalizedName(
                programDefinition.localizedName().updateTranslation(locale, displayName))
            .setLocalizedDescription(
                programDefinition
                    .localizedDescription()
                    .updateTranslation(locale, displayDescription))
            .build()
            .toProgram();
    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  // Program names and program URL slugs must be unique in a given CiviForm
  // system. If the slugs of two names collide, the names also collide, so
  // we can check both by just checking for slug collisions.
  // For more info on URL slugs see: https://en.wikipedia.org/wiki/Clean_URL#Slug
  private boolean hasProgramNameCollision(String programName) {
    Slugify slugifier = new Slugify();
    return getAllProgramNames().stream()
        .map(slugifier::slugify)
        .anyMatch(slugifier.slugify(programName)::equals);
  }

  private void validateProgramText(
      ImmutableSet.Builder<CiviFormError> builder, String fieldName, String text) {
    if (text.isBlank()) {
      builder.add(CiviFormError.of("program " + fieldName.trim() + " cannot be blank"));
    }
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addBlockToProgram(long programId)
      throws ProgramNotFoundException {
    try {
      return addBlockToProgram(programId, Optional.empty());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(
          "The ProgramBlockDefinitionNotFoundException should never be thrown when the enumerator"
              + " id is empty.");
    }
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addRepeatedBlockToProgram(
      long programId, long enumeratorBlockId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    return addBlockToProgram(programId, Optional.of(enumeratorBlockId));
  }

  private ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addBlockToProgram(
      long programId, Optional<Long> enumeratorBlockId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    if (enumeratorBlockId.isPresent()
        && !programDefinition.hasEnumerator(enumeratorBlockId.get())) {
      throw new ProgramBlockDefinitionNotFoundException(programId, enumeratorBlockId.get());
    }

    long blockId = getNextBlockId(programDefinition);
    String blockName;
    if (enumeratorBlockId.isPresent()) {
      blockName = String.format("Screen %d (repeated from %d)", blockId, enumeratorBlockId.get());
    } else {
      blockName = String.format("Screen %d", blockId);
    }
    String blockDescription =
        "What is the purpose of this screen? Add a description that summarizes the information"
            + " collected.";

    ImmutableSet<CiviFormError> errors = validateBlockDefinition(blockName, blockDescription);
    if (!errors.isEmpty()) {
      return ErrorAnd.errorAnd(
          errors, ProgramBlockAdditionResult.of(programDefinition, Optional.empty()));
    }

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(blockId)
            .setName(blockName)
            .setDescription(blockDescription)
            .setEnumeratorId(enumeratorBlockId)
            .build();
    Program program =
        programDefinition.insertBlockDefinitionInTheRightPlace(blockDefinition).toProgram();
    ProgramDefinition updatedProgram =
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join();
    BlockDefinition updatedBlockDefinition = updatedProgram.getBlockDefinition(blockId);
    return ErrorAnd.of(
        ProgramBlockAdditionResult.of(updatedProgram, Optional.of(updatedBlockDefinition)));
  }

  @Override
  @Transactional
  public ProgramDefinition moveBlock(
      long programId, long blockId, ProgramDefinition.Direction direction)
      throws ProgramNotFoundException, IllegalPredicateOrderingException {
    Program program;
    try {
      program = getProgramDefinition(programId).moveBlock(blockId, direction).toProgram();
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(
          "Something happened to the program's block while trying to move it", e);
    }
    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program).getProgramDefinition())
        .toCompletableFuture()
        .join();
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> updateBlock(
      long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
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

    try {
      return ErrorAnd.of(
          updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition));
    } catch (IllegalPredicateOrderingException e) {
      // Updating a block's metadata should never invalidate a predicate.
      throw new RuntimeException(
          "Unexpected error: updating this block invalidated a block condition");
    }
  }

  private ImmutableSet<CiviFormError> validateBlockDefinition(String name, String description) {
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.builder();
    if (name.isBlank()) {
      errors.add(CiviFormError.of("screen name cannot be blank"));
    }
    if (description.isBlank()) {
      errors.add(CiviFormError.of("screen description cannot be blank"));
    }
    return errors.build();
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockQuestions(
      long programId,
      long blockDefinitionId,
      ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
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
          ProgramBlockDefinitionNotFoundException {
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
          ProgramQuestionDefinition.create(
              roQuestionService.getQuestionDefinition(qid), Optional.of(programId)));
    }

    ImmutableList<ProgramQuestionDefinition> newProgramQuestionDefinitions =
        newQuestionListBuilder.build();

    blockDefinition =
        blockDefinition.toBuilder()
            .setProgramQuestionDefinitions(newProgramQuestionDefinitions)
            .build();
    try {
      return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
    } catch (IllegalPredicateOrderingException e) {
      // This should never happen
      throw new RuntimeException(
          String.format(
              "Unexpected error: Adding a question to block %s invalidated a predicate",
              blockDefinition.name()));
    }
  }

  @Override
  @Transactional
  public ProgramDefinition removeQuestionsFromBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws QuestionNotFoundException, ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException, IllegalPredicateOrderingException {
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
  public ProgramDefinition setBlockPredicate(
      long programId, long blockDefinitionId, @Nullable PredicateDefinition predicate)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setVisibilityPredicate(Optional.ofNullable(predicate))
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition removeBlockPredicate(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    try {
      return setBlockPredicate(programId, blockDefinitionId, null);
    } catch (IllegalPredicateOrderingException e) {
      // Removing a predicate should never invalidate another.
      throw new RuntimeException("Unexpected error: removing this predicate invalidates another");
    }
  }

  @Override
  @Transactional
  public ProgramDefinition setProgramQuestionDefinitionOptionality(
      long programId, long blockDefinitionId, long questionDefinitionId, boolean optional)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

    if (!blockDefinition.programQuestionDefinitions().stream()
        .anyMatch(pqd -> pqd.id() == questionDefinitionId)) {
      throw new ProgramQuestionDefinitionNotFoundException(
          programId, blockDefinitionId, questionDefinitionId);
    }

    ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions =
        blockDefinition.programQuestionDefinitions().stream()
            .map(pqd -> pqd.id() == questionDefinitionId ? pqd.setOptional(optional) : pqd)
            .collect(ImmutableList.toImmutableList());

    try {
      return updateProgramDefinitionWithBlockDefinition(
          programDefinition,
          blockDefinition.toBuilder()
              .setProgramQuestionDefinitions(programQuestionDefinitions)
              .build());
    } catch (IllegalPredicateOrderingException e) {
      // Changing a question between required and optional should not affect predicates. If a
      // question is optional and a predicate depends on its answer, the predicate will be false.
      throw new RuntimeException(
          "Unexpected error: updating this question invalidated a block condition");
    }
  }

  @Override
  @Transactional
  public ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramNeedsABlockException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    ImmutableList<BlockDefinition> newBlocks =
        programDefinition.blockDefinitions().stream()
            .filter(block -> block.id() != blockDefinitionId)
            .collect(ImmutableList.toImmutableList());
    if (newBlocks.isEmpty()) {
      throw new ProgramNeedsABlockException(programId);
    }

    return updateProgramDefinitionWithBlockDefinitions(programDefinition, newBlocks);
  }

  @Override
  public ImmutableList<Application> getSubmittedProgramApplications(long programId)
      throws ProgramNotFoundException {
    Optional<Program> programMaybe =
        programRepository.lookupProgram(programId).toCompletableFuture().join();
    if (programMaybe.isEmpty()) {
      throw new ProgramNotFoundException(programId);
    }
    return programMaybe.get().getSubmittedApplications();
  }

  @Override
  public PaginationResult<Application> getSubmittedProgramApplicationsAllVersions(
      long programId, PaginationSpec paginationSpec, Optional<String> searchNameFragment) {
    return programRepository.getApplicationsForAllProgramVersions(
        programId, paginationSpec, searchNameFragment);
  }

  @Override
  public ProgramDefinition newDraftOf(long id) throws ProgramNotFoundException {
    // Note: It's unclear that we actually want to update an existing draft this way, as it would
    // effectively reset the  draft which is not part of any user flow. Given the interdependency of
    // draft updates this is likely to cause issues as in #2179.
    return programRepository
        .createOrUpdateDraft(this.getProgramDefinition(id).toProgram())
        .getProgramDefinition();
  }

  @Override
  public ImmutableList<String> getNotificationEmailAddresses(String programName) {
    ImmutableList<String> explicitProgramAdmins =
        programRepository.getProgramAdministrators(programName).stream()
            .map(Account::getEmailAddress)
            .filter(address -> !Strings.isNullOrEmpty(address))
            .collect(ImmutableList.toImmutableList());
    // If there are any program admins, return them.
    if (explicitProgramAdmins.size() > 0) {
      return explicitProgramAdmins;
    }
    // Return all the global admins email addresses.
    return userRepository.getGlobalAdmins().stream()
        .map(Account::getEmailAddress)
        .filter(address -> !Strings.isNullOrEmpty(address))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public ImmutableList<Program> getOtherProgramVersions(long programId) {
    return programRepository.getAllProgramVersions(programId).stream()
        .filter(program -> program.id != programId)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public ImmutableList<ProgramDefinition> getAllProgramDefinitionVersions(long programId) {
    return programRepository.getAllProgramVersions(programId).stream()
        .map(program -> syncProgramAssociations(program).toCompletableFuture().join())
        .collect(ImmutableList.toImmutableList());
  }

  private ProgramDefinition updateProgramDefinitionWithBlockDefinitions(
      ProgramDefinition programDefinition, ImmutableList<BlockDefinition> blocks)
      throws IllegalPredicateOrderingException {
    ProgramDefinition program = programDefinition.toBuilder().setBlockDefinitions(blocks).build();

    if (!program.hasValidPredicateOrdering()) {
      throw new IllegalPredicateOrderingException("This action would invalidate a block condition");
    }

    return syncProgramDefinitionQuestions(
            programRepository.updateProgramSync(program.toProgram()).getProgramDefinition())
        .toCompletableFuture()
        .join();
  }

  private ProgramDefinition updateProgramDefinitionWithBlockDefinition(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition)
      throws IllegalPredicateOrderingException {

    ImmutableList<BlockDefinition> updatedBlockDefinitions =
        programDefinition.blockDefinitions().stream()
            .map(b -> b.id() == blockDefinition.id() ? blockDefinition : b)
            .collect(ImmutableList.toImmutableList());

    return updateProgramDefinitionWithBlockDefinitions(programDefinition, updatedBlockDefinitions);
  }

  private long getNextBlockId(ProgramDefinition programDefinition) {
    return programDefinition.getMaxBlockDefinitionId() + 1;
  }

  private boolean isActiveOrDraftProgram(Program program) {
    return Streams.concat(
            versionRepository.getActiveVersion().getPrograms().stream(),
            versionRepository.getDraftVersion().getPrograms().stream())
        .anyMatch(p -> p.id.equals(program.id));
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
            roQuestionService -> {
              try {
                return syncProgramDefinitionQuestions(programDefinition, roQuestionService);
              } catch (QuestionNotFoundException e) {
                throw new RuntimeException(
                    String.format("Question not found for Program %s", programDefinition.id()), e);
              }
            },
            httpExecutionContext.current());
  }

  private ProgramDefinition syncProgramDefinitionQuestions(
      ProgramDefinition programDefinition, Version version) {
    try {
      return syncProgramDefinitionQuestions(
          programDefinition, questionService.getReadOnlyVersionedQuestionService(version));
    } catch (QuestionNotFoundException e) {
      throw new RuntimeException(
          String.format(
              "Question not found for Program %s at Version %s",
              programDefinition.id(), version.id),
          e);
    }
  }

  private ProgramDefinition syncProgramDefinitionQuestions(
      ProgramDefinition programDefinition, ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException {
    ProgramDefinition.Builder programDefinitionBuilder = programDefinition.toBuilder();
    ImmutableList.Builder<BlockDefinition> blockListBuilder = ImmutableList.builder();

    for (BlockDefinition block : programDefinition.blockDefinitions()) {
      BlockDefinition syncedBlock =
          syncBlockDefinitionQuestions(programDefinition.id(), block, roQuestionService);
      blockListBuilder.add(syncedBlock);
    }

    programDefinitionBuilder.setBlockDefinitions(blockListBuilder.build());
    return programDefinitionBuilder.build();
  }

  private BlockDefinition syncBlockDefinitionQuestions(
      long programDefinitionId,
      BlockDefinition blockDefinition,
      ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException {
    BlockDefinition.Builder blockBuilder = blockDefinition.toBuilder();

    ImmutableList.Builder<ProgramQuestionDefinition> pqdListBuilder = ImmutableList.builder();
    for (ProgramQuestionDefinition pqd : blockDefinition.programQuestionDefinitions()) {
      ProgramQuestionDefinition syncedPqd =
          syncProgramQuestionDefinition(programDefinitionId, pqd, roQuestionService);
      pqdListBuilder.add(syncedPqd);
    }

    blockBuilder.setProgramQuestionDefinitions(pqdListBuilder.build());
    return blockBuilder.build();
  }

  private ProgramQuestionDefinition syncProgramQuestionDefinition(
      long programDefinitionId,
      ProgramQuestionDefinition pqd,
      ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException {
    QuestionDefinition questionDefinition = roQuestionService.getQuestionDefinition(pqd.id());
    return pqd.loadCompletely(programDefinitionId, questionDefinition);
  }
}
