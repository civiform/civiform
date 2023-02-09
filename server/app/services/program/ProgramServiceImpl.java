package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import controllers.BadRequestException;
import forms.BlockForm;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import models.Account;
import models.Application;
import models.DisplayMode;
import models.Program;
import models.Version;
import modules.MainModule;
import play.db.ebean.Transactional;
import play.libs.F;
import play.libs.concurrent.HttpExecutionContext;
import repository.ProgramRepository;
import repository.SubmittedApplicationFilter;
import repository.UserRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.IdentifierBasedPaginationSpec;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.ProgramBlockValidation;
import services.ProgramBlockValidation.AddQuestionResult;
import services.program.predicate.PredicateDefinition;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

/** Implementation class for {@link ProgramService} interface. */
public final class ProgramServiceImpl implements ProgramService {

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
      return getActiveProgramDefinitionAsync(id).toCompletableFuture().join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof ProgramNotFoundException) {
        throw new ProgramNotFoundException(id);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public ActiveAndDraftPrograms getActiveAndDraftPrograms() {
    return ActiveAndDraftPrograms.buildFromCurrentVersions(this, versionRepository);
  }

  @Override
  public CompletionStage<ProgramDefinition> getActiveProgramDefinitionAsync(long id) {
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

  @Override
  public CompletionStage<ProgramDefinition> getActiveProgramDefinitionAsync(String programSlug) {
    return programRepository
        .getForSlug(programSlug)
        .thenComposeAsync(this::syncProgramAssociations, httpExecutionContext.current());
  }

  private CompletionStage<ProgramDefinition> syncProgramAssociations(Program program) {
    if (isActiveOrDraftProgram(program)) {
      return syncProgramDefinitionQuestions(program.getProgramDefinition())
          .thenApply(ProgramDefinition::orderBlockDefinitions);
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
        .map(MainModule.SLUGIFIER::slugify)
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

    if (defaultDisplayName.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A public display name for the program is required"));
    }
    if (defaultDisplayDescription.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A public description for the program is required"));
    }
    if (adminName.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A program URL is required"));
    } else if (!MainModule.SLUGIFIER.slugify(adminName).equals(adminName)) {
      errorsBuilder.add(
          CiviFormError.of(
              "A program URL may only contain lowercase letters, numbers, and dashes"));
    } else if (hasProgramNameCollision(adminName)) {
      errorsBuilder.add(CiviFormError.of("A program URL of " + adminName + " already exists"));
    }
    if (displayMode.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A program visibility option must be selected"));
    }
    if (adminDescription.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A program note is required"));
    }

    ImmutableSet<CiviFormError> errors = errorsBuilder.build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    ErrorAnd<BlockDefinition, CiviFormError> maybeEmptyBlock =
        createEmptyBlockDefinition(
            /* blockId= */ 1, /* maybeEnumeratorBlockId= */ Optional.empty());
    if (maybeEmptyBlock.isError()) {
      return ErrorAnd.error(maybeEmptyBlock.getErrors());
    }
    BlockDefinition emptyBlock = maybeEmptyBlock.getResult();

    Program program =
        new Program(
            adminName,
            adminDescription,
            defaultDisplayName,
            defaultDisplayDescription,
            externalLink,
            displayMode,
            ImmutableList.of(emptyBlock),
            versionRepository.getDraftVersion(),
            ProgramType.DEFAULT);

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
    if (displayName.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A public display name for the program is required"));
    }
    if (displayDescription.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A public description for the program is required"));
    }
    if (displayMode.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A program visibility option must be selected"));
    }
    if (adminDescription.isBlank()) {
      errorsBuilder.add(CiviFormError.of("A program note is required"));
    }
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

  /**
   * Determines whether the list of provided localized application status updates exactly correspond
   * to the list of configured application statuses within the program. This means that:
   * <li>The lists are of the same length
   * <li>Have the exact same ordering of statuses
   */
  private void validateLocalizationStatuses(
      LocalizationUpdate localizationUpdate, ProgramDefinition program)
      throws OutOfDateStatusesException {
    ImmutableList<String> localizationStatusNames =
        localizationUpdate.statuses().stream()
            .map(LocalizationUpdate.StatusUpdate::statusKeyToUpdate)
            .collect(ImmutableList.toImmutableList());
    ImmutableList<String> configuredStatusNames =
        program.statusDefinitions().getStatuses().stream()
            .map(StatusDefinitions.Status::statusText)
            .collect(ImmutableList.toImmutableList());
    if (!localizationStatusNames.equals(configuredStatusNames)) {
      throw new OutOfDateStatusesException();
    }
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> updateLocalization(
      long programId, Locale locale, LocalizationUpdate localizationUpdate)
      throws ProgramNotFoundException, OutOfDateStatusesException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();
    validateProgramText(errorsBuilder, "display name", localizationUpdate.localizedDisplayName());
    validateProgramText(
        errorsBuilder, "display description", localizationUpdate.localizedDisplayDescription());

    validateLocalizationStatuses(localizationUpdate, programDefinition);

    // We iterate the existing statuses along with the provided statuses since they were verified
    // to be consistently ordered above.
    ImmutableList.Builder<StatusDefinitions.Status> toUpdateStatusesBuilder =
        ImmutableList.builder();
    for (int statusIdx = 0;
        statusIdx < programDefinition.statusDefinitions().getStatuses().size();
        statusIdx++) {
      LocalizationUpdate.StatusUpdate statusUpdateData =
          localizationUpdate.statuses().get(statusIdx);
      StatusDefinitions.Status existingStatus =
          programDefinition.statusDefinitions().getStatuses().get(statusIdx);
      StatusDefinitions.Status.Builder updateBuilder =
          existingStatus.toBuilder()
              .setLocalizedStatusText(
                  existingStatus
                      .localizedStatusText()
                      .updateTranslation(locale, statusUpdateData.localizedStatusText()));
      // If the status has email content, update the localization to whatever was provided;
      // otherwise if there's a localization update when there is no email content to
      // localize, that indicates a mismatch between the frontend and the database.
      if (existingStatus.localizedEmailBodyText().isPresent()) {
        updateBuilder.setLocalizedEmailBodyText(
            Optional.of(
                existingStatus
                    .localizedEmailBodyText()
                    .get()
                    .updateTranslation(locale, statusUpdateData.localizedEmailBody())));
      } else if (statusUpdateData.localizedEmailBody().isPresent()) {
        throw new OutOfDateStatusesException();
      }
      toUpdateStatusesBuilder.add(updateBuilder.build());
    }

    ImmutableSet<CiviFormError> errors = errorsBuilder.build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    Program program =
        programDefinition.toBuilder()
            .setLocalizedName(
                programDefinition
                    .localizedName()
                    .updateTranslation(locale, localizationUpdate.localizedDisplayName()))
            .setLocalizedDescription(
                programDefinition
                    .localizedDescription()
                    .updateTranslation(locale, localizationUpdate.localizedDisplayDescription()))
            .setStatusDefinitions(
                programDefinition.statusDefinitions().setStatuses(toUpdateStatusesBuilder.build()))
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
    return getAllProgramNames().stream()
        .map(MainModule.SLUGIFIER::slugify)
        .anyMatch(MainModule.SLUGIFIER.slugify(programName)::equals);
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

  private static ErrorAnd<BlockDefinition, CiviFormError> createEmptyBlockDefinition(
      long blockId, Optional<Long> maybeEnumeratorBlockId) {
    String blockName =
        maybeEnumeratorBlockId.isPresent()
            ? String.format("Screen %d (repeated from %d)", blockId, maybeEnumeratorBlockId.get())
            : String.format("Screen %d", blockId);
    String blockDescription = String.format("Screen %d description", blockId);
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(blockId)
            .setName(blockName)
            .setDescription(blockDescription)
            .setEnumeratorId(maybeEnumeratorBlockId)
            .build();
    ImmutableSet<CiviFormError> errors = validateBlockDefinition(blockDefinition);
    return errors.isEmpty() ? ErrorAnd.of(blockDefinition) : ErrorAnd.error(errors);
  }

  private ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addBlockToProgram(
      long programId, Optional<Long> enumeratorBlockId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    if (enumeratorBlockId.isPresent()
        && !programDefinition.hasEnumerator(enumeratorBlockId.get())) {
      throw new ProgramBlockDefinitionNotFoundException(programId, enumeratorBlockId.get());
    }

    ErrorAnd<BlockDefinition, CiviFormError> maybeBlockDefinition =
        createEmptyBlockDefinition(getNextBlockId(programDefinition), enumeratorBlockId);
    if (maybeBlockDefinition.isError()) {
      return ErrorAnd.errorAnd(
          maybeBlockDefinition.getErrors(),
          ProgramBlockAdditionResult.of(programDefinition, Optional.empty()));
    }
    BlockDefinition blockDefinition = maybeBlockDefinition.getResult();
    Program program =
        programDefinition.insertBlockDefinitionInTheRightPlace(blockDefinition).toProgram();
    ProgramDefinition updatedProgram =
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join();
    BlockDefinition updatedBlockDefinition =
        updatedProgram.getBlockDefinition(blockDefinition.id());
    return ErrorAnd.of(
        ProgramBlockAdditionResult.of(updatedProgram, Optional.of(updatedBlockDefinition)));
  }

  @Override
  @Transactional
  public ProgramDefinition moveBlock(
      long programId, long blockId, ProgramDefinition.Direction direction)
      throws ProgramNotFoundException, IllegalPredicateOrderingException {
    final Program program;
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
  public ErrorAnd<ProgramDefinition, CiviFormError> appendStatus(
      long programId, StatusDefinitions.Status status)
      throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = getProgramDefinition(programId);
    if (program.statusDefinitions().getStatuses().stream()
        .filter(s -> s.statusText().equals(status.statusText()))
        .findAny()
        .isPresent()) {
      throw new DuplicateStatusException(status.statusText());
    }
    program
        .statusDefinitions()
        .setStatuses(
            ImmutableList.<StatusDefinitions.Status>builder()
                .addAll(program.statusDefinitions().getStatuses())
                .add(status)
                .build());

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program.toProgram()).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> editStatus(
      long programId,
      String toReplaceStatusName,
      Function<StatusDefinitions.Status, StatusDefinitions.Status> statusReplacer)
      throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = getProgramDefinition(programId);
    ImmutableMap<String, Integer> statusNameToIndex =
        statusNameToIndexMap(program.statusDefinitions().getStatuses());
    if (!statusNameToIndex.containsKey(toReplaceStatusName)) {
      return ErrorAnd.error(
          ImmutableSet.of(
              CiviFormError.of(
                  "The status being edited no longer exists and may have been modified in a"
                      + " separate window.")));
    }
    List<StatusDefinitions.Status> statusesCopy =
        Lists.newArrayList(program.statusDefinitions().getStatuses());
    StatusDefinitions.Status editedStatus =
        statusReplacer.apply(statusesCopy.get(statusNameToIndex.get(toReplaceStatusName)));
    // If the status name was changed and it matches another status, issue an error.
    if (!toReplaceStatusName.equals(editedStatus.statusText())
        && statusNameToIndex.containsKey(editedStatus.statusText())) {
      throw new DuplicateStatusException(editedStatus.statusText());
    }
    statusesCopy.set(statusNameToIndex.get(toReplaceStatusName), editedStatus);
    program.statusDefinitions().setStatuses(ImmutableList.copyOf(statusesCopy));

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program.toProgram()).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> deleteStatus(
      long programId, String toRemoveStatusName) throws ProgramNotFoundException {
    ProgramDefinition program = getProgramDefinition(programId);
    ImmutableMap<String, Integer> statusNameToIndex =
        statusNameToIndexMap(program.statusDefinitions().getStatuses());
    if (!statusNameToIndex.containsKey(toRemoveStatusName)) {
      return ErrorAnd.error(
          ImmutableSet.of(
              CiviFormError.of(
                  "The status being deleted no longer exists and may have been deleted in a"
                      + " separate window.")));
    }
    List<StatusDefinitions.Status> statusesCopy =
        Lists.newArrayList(program.statusDefinitions().getStatuses());
    statusesCopy.remove(statusNameToIndex.get(toRemoveStatusName).intValue());
    program.statusDefinitions().setStatuses(ImmutableList.copyOf(statusesCopy));

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program.toProgram()).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  private static ImmutableMap<String, Integer> statusNameToIndexMap(
      ImmutableList<StatusDefinitions.Status> statuses) {
    return IntStream.range(0, statuses.size())
        .boxed()
        .collect(ImmutableMap.toImmutableMap(i -> statuses.get(i).statusText(), i -> i));
  }

  @Override
  @Transactional
  public ErrorAnd<ProgramDefinition, CiviFormError> updateBlock(
      long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setName(blockForm.getName())
            .setDescription(blockForm.getDescription())
            .build();
    ImmutableSet<CiviFormError> errors = validateBlockDefinition(blockDefinition);
    if (!errors.isEmpty()) {
      return ErrorAnd.errorAnd(errors, programDefinition);
    }

    try {
      return ErrorAnd.of(
          updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition));
    } catch (IllegalPredicateOrderingException e) {
      // Updating a block's metadata should never invalidate a predicate.
      throw new RuntimeException(
          "Unexpected error: updating this block invalidated a block condition");
    }
  }

  private static ImmutableSet<CiviFormError> validateBlockDefinition(
      BlockDefinition blockDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.builder();
    if (blockDefinition.name().isBlank()) {
      errors.add(CiviFormError.of("screen name cannot be blank"));
    }
    if (blockDefinition.description().isBlank()) {
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
      throws CantAddQuestionToBlockException, QuestionNotFoundException, ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

    ImmutableList.Builder<ProgramQuestionDefinition> updatedBlockQuestions =
        ImmutableList.builder();
    // Add existing block questions.
    updatedBlockQuestions.addAll(blockDefinition.programQuestionDefinitions());

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    for (long questionId : questionIds) {
      ProgramQuestionDefinition question =
          ProgramQuestionDefinition.create(
              roQuestionService.getQuestionDefinition(questionId), Optional.of(programId));
      ProgramBlockValidation.AddQuestionResult canAddQuestion =
          ProgramBlockValidation.canAddQuestion(
              programDefinition, blockDefinition, question.getQuestionDefinition());
      if (canAddQuestion != AddQuestionResult.ELIGIBLE) {
        throw new CantAddQuestionToBlockException(
            programDefinition, blockDefinition, question.getQuestionDefinition(), canAddQuestion);
      }
      updatedBlockQuestions.add(question);
    }

    blockDefinition =
        blockDefinition.toBuilder()
            .setProgramQuestionDefinitions(updatedBlockQuestions.build())
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
  public ProgramDefinition setBlockVisibilityPredicate(
      long programId, long blockDefinitionId, Optional<PredicateDefinition> predicate)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setVisibilityPredicate(predicate)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition setBlockEligibilityDefinition(
      long programId, long blockDefinitionId, Optional<EligibilityDefinition> eligibility)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setEligibilityDefinition(eligibility)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  @Override
  @Transactional
  public ProgramDefinition removeBlockPredicate(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    try {
      return setBlockVisibilityPredicate(
          programId, blockDefinitionId, /* predicate= */ Optional.empty());
    } catch (IllegalPredicateOrderingException e) {
      // Removing a predicate should never invalidate another.
      throw new RuntimeException("Unexpected error: removing this predicate invalidates another");
    }
  }

  @Override
  @Transactional
  public ProgramDefinition removeBlockEligibilityPredicate(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    try {
      return setBlockEligibilityDefinition(
          programId, blockDefinitionId, /* eligibility= */ Optional.empty());
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
  public ProgramDefinition setProgramQuestionDefinitionAddressCorrectionEnabled(
      long programId,
      long blockDefinitionId,
      long questionDefinitionId,
      boolean addressCorrectionEnabled)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

    if (!blockDefinition.programQuestionDefinitions().stream()
        .anyMatch(pqd -> pqd.id() == questionDefinitionId)) {
      throw new ProgramQuestionDefinitionNotFoundException(
          programId, blockDefinitionId, questionDefinitionId);
    }

    if (!blockDefinition.hasAddress()) {
      throw new BadRequestException(
          "Unexpected error: updating a non address question with address correction enabled");
    }

    if (!addressCorrectionEnabled
        && programDefinition.isQuestionUsedInPredicate(questionDefinitionId)) {
      throw new BadRequestException(
          String.format("Cannot disable correction for an address used in a predicate."));
    }

    ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions =
        blockDefinition.programQuestionDefinitions().stream()
            .map(
                pqd ->
                    pqd.id() == questionDefinitionId
                        ? pqd.setAddressCorrectionEnabled(addressCorrectionEnabled)
                        : pqd)
            .collect(ImmutableList.toImmutableList());

    try {
      return updateProgramDefinitionWithBlockDefinition(
          programDefinition,
          blockDefinition.toBuilder()
              .setProgramQuestionDefinitions(programQuestionDefinitions)
              .build());
    } catch (IllegalPredicateOrderingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Transactional
  public ProgramDefinition setProgramQuestionDefinitionPosition(
      long programId, long blockDefinitionId, long questionDefinitionId, int newPosition)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException, InvalidQuestionPositionException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

    ImmutableList<ProgramQuestionDefinition> questions =
        blockDefinition.programQuestionDefinitions();

    if (newPosition < 0 || newPosition >= questions.size()) {
      throw InvalidQuestionPositionException.positionOutOfBounds(newPosition, questions.size());
    }

    // move question to the new position
    Optional<ProgramQuestionDefinition> toMove =
        questions.stream().filter(q -> q.id() == questionDefinitionId).findFirst();
    if (toMove.isEmpty()) {
      throw new ProgramQuestionDefinitionNotFoundException(
          programId, blockDefinitionId, questionDefinitionId);
    }
    List<ProgramQuestionDefinition> otherQuestions =
        questions.stream().filter(q -> q.id() != questionDefinitionId).collect(Collectors.toList());
    otherQuestions.add(newPosition, toMove.get());

    try {
      return updateProgramDefinitionWithBlockDefinition(
          programDefinition,
          blockDefinition.toBuilder()
              .setProgramQuestionDefinitions(ImmutableList.copyOf(otherQuestions))
              .build());
    } catch (IllegalPredicateOrderingException e) {
      // Changing a question position within block should not affect predicates
      // because predicates cannot depend on questions within the same block.
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
      long programId,
      F.Either<IdentifierBasedPaginationSpec<Long>, PageNumberBasedPaginationSpec>
          paginationSpecEither,
      SubmittedApplicationFilter filters) {
    return programRepository.getApplicationsForAllProgramVersions(
        programId, paginationSpecEither, filters);
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
    // Note: This method is also used for non question updates.  It'd likely be
    // good to have a focused method for that.
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
