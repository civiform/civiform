package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProgramAcls;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import controllers.BadRequestException;
import forms.BlockForm;
import java.util.HashSet;
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
import services.LocalizedStrings;
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

  private static final String MISSING_DISPLAY_NAME_MSG =
      "A public display name for the program is required";
  private static final String MISSING_DISPLAY_DESCRIPTION_MSG =
      "A public description for the program is required";
  private static final String MISSING_DISPLAY_MODE_MSG =
      "A program visibility option must be selected";
  private static final String MISSING_ADMIN_DESCRIPTION_MSG = "A program note is required";
  private static final String MISSING_ADMIN_NAME_MSG = "A program URL is required";
  private static final String INVALID_ADMIN_NAME_MSG =
      "A program URL may only contain lowercase letters, numbers, and dashes";
  private static final String INVALID_PROGRAM_LINK_FORMAT_MSG =
      "A program link must begin with 'http://' or 'https://'";
  private static final String MISSING_TI_ORGS_FOR_THE_DISPLAY_MODE =
      "One or more TI Org must be selected for program visibility";

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
  public ImmutableSet<String> getActiveProgramNames() {
    return versionRepository.getActiveVersion().getProgramsNames();
  }

  @Override
  public ActiveAndDraftPrograms getActiveAndDraftPrograms() {
    return ActiveAndDraftPrograms.buildFromCurrentVersionsSynced(this, versionRepository);
  }

  @Override
  public ActiveAndDraftPrograms getActiveAndDraftProgramsWithoutQuestionLoad() {
    return ActiveAndDraftPrograms.buildFromCurrentVersionsUnsynced(versionRepository);
  }

  @Override
  public Optional<ProgramDefinition> getCommonIntakeForm() {
    return getActiveAndDraftPrograms().getMostRecentProgramDefinitions().stream()
        .filter(ProgramDefinition::isCommonIntakeForm)
        .findFirst();
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
        .getActiveProgramFromSlug(programSlug)
        .thenComposeAsync(this::syncProgramAssociations, httpExecutionContext.current());
  }

  @Override
  public ImmutableList<ProgramDefinition> getAllProgramDefinitionVersions(long programId) {
    return programRepository.getAllProgramVersions(programId).stream()
        .map(program -> syncProgramAssociations(program).toCompletableFuture().join())
        .collect(ImmutableList.toImmutableList());
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

  private boolean isActiveOrDraftProgram(Program program) {
    return Streams.concat(
            versionRepository.getActiveVersion().getPrograms().stream(),
            versionRepository.getDraftVersion().getPrograms().stream())
        .anyMatch(p -> p.id.equals(program.id));
  }

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> createProgramDefinition(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription,
      String defaultConfirmationMessage,
      String externalLink,
      String displayMode,
      ProgramType programType,
      Boolean isIntakeFormFeatureEnabled,
      ImmutableList<Long> tiGroups) {
    ImmutableSet<CiviFormError> errors =
        validateProgramDataForCreate(
            adminName,
            adminDescription,
            defaultDisplayName,
            defaultDisplayDescription,
            externalLink,
            displayMode,
            tiGroups);
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    ErrorAnd<BlockDefinition, CiviFormError> maybeEmptyBlock =
        createEmptyBlockDefinition(
            /* blockId= */ 1, /* maybeEnumeratorBlockId= */ Optional.empty());
    if (maybeEmptyBlock.isError()) {
      return ErrorAnd.error(maybeEmptyBlock.getErrors());
    }

    if (!isIntakeFormFeatureEnabled) {
      programType = ProgramType.DEFAULT;
    }
    if (programType.equals(ProgramType.COMMON_INTAKE_FORM) && getCommonIntakeForm().isPresent()) {
      clearCommonIntakeForm();
    }
    ProgramAcls programAcls = new ProgramAcls(new HashSet<>(tiGroups));
    BlockDefinition emptyBlock = maybeEmptyBlock.getResult();
    Program program =
        new Program(
            adminName,
            adminDescription,
            defaultDisplayName,
            defaultDisplayDescription,
            defaultConfirmationMessage,
            externalLink,
            displayMode,
            ImmutableList.of(emptyBlock),
            versionRepository.getDraftVersion(),
            programType,
            programAcls);

    return ErrorAnd.of(programRepository.insertProgramSync(program).getProgramDefinition());
  }

  @Override
  public ImmutableSet<CiviFormError> validateProgramDataForCreate(
      String adminName,
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode,
      ImmutableList<Long> tiGroups) {
    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();
    errorsBuilder.addAll(
        validateProgramData(
            adminDescription,
            displayName,
            displayDescription,
            externalLink,
            displayMode,
            tiGroups));
    if (adminName.isBlank()) {
      errorsBuilder.add(CiviFormError.of(MISSING_ADMIN_NAME_MSG));
    } else if (!MainModule.SLUGIFIER.slugify(adminName).equals(adminName)) {
      errorsBuilder.add(CiviFormError.of(INVALID_ADMIN_NAME_MSG));
    } else if (hasProgramNameCollision(adminName)) {
      errorsBuilder.add(CiviFormError.of("A program URL of " + adminName + " already exists"));
    }
    return errorsBuilder.build();
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

  @Override
  public ErrorAnd<ProgramDefinition, CiviFormError> updateProgramDefinition(
      long programId,
      Locale locale,
      String adminDescription,
      String displayName,
      String displayDescription,
      String confirmationMessage,
      String externalLink,
      String displayMode,
      ProgramType programType,
      Boolean isIntakeFormFeatureEnabled,
      ImmutableList<Long> tiGroups)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    ImmutableSet<CiviFormError> errors =
        validateProgramDataForUpdate(
            adminDescription, displayName, displayDescription, externalLink, displayMode, tiGroups);
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    if (!isIntakeFormFeatureEnabled) {
      programType = ProgramType.DEFAULT;
    }
    if (programType.equals(ProgramType.COMMON_INTAKE_FORM)) {
      Optional<ProgramDefinition> maybeCommonIntakeForm = getCommonIntakeForm();
      if (maybeCommonIntakeForm.isPresent()
          && !programDefinition.adminName().equals(maybeCommonIntakeForm.get().adminName())) {
        clearCommonIntakeForm();
      }
    }

    LocalizedStrings newConfirmationMessageTranslations =
        maybeClearConfirmationMessageTranslations(programDefinition, locale, confirmationMessage);

    if (programType.equals(ProgramType.COMMON_INTAKE_FORM)
        && !programDefinition.isCommonIntakeForm()) {
      programDefinition = removeAllEligibilityPredicates(programDefinition);
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
            .setLocalizedConfirmationMessage(newConfirmationMessageTranslations)
            .setExternalLink(externalLink)
            .setDisplayMode(DisplayMode.valueOf(displayMode))
            .setProgramType(programType)
            .setAcls(new ProgramAcls(new HashSet<>(tiGroups)))
            .build()
            .toProgram();

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  /** Removes eligibility predicates from all blocks in this program. */
  private ProgramDefinition removeAllEligibilityPredicates(ProgramDefinition programDefinition)
      throws ProgramNotFoundException {
    try {
      return updateProgramDefinitionWithBlockDefinitions(
          programDefinition,
          programDefinition.blockDefinitions().stream()
              .map(block -> block.toBuilder().setEligibilityDefinition(Optional.empty()).build())
              .collect(ImmutableList.toImmutableList()));
    } catch (IllegalPredicateOrderingException e) {
      throw new RuntimeException("Unexpected error: removing this predicate invalidates another");
    }
  }

  /**
   * When an admin deletes a custom confirmation screen, we want to also clear out all associated
   * translations
   */
  private LocalizedStrings maybeClearConfirmationMessageTranslations(
      ProgramDefinition programDefinition, Locale locale, String confirmationMessage) {
    LocalizedStrings existingConfirmationMessageTranslations =
        programDefinition.localizedConfirmationMessage();
    LocalizedStrings newConfirmationMessageTranslations;
    if (locale.equals(Locale.US) && confirmationMessage.equals("")) {
      newConfirmationMessageTranslations = LocalizedStrings.create(ImmutableMap.of(Locale.US, ""));
    } else {
      newConfirmationMessageTranslations =
          existingConfirmationMessageTranslations.updateTranslation(locale, confirmationMessage);
    }
    return newConfirmationMessageTranslations;
  }

  /**
   * Clears the common intake form if it exists.
   *
   * <p>If there is a program among the most recent versions of all programs marked as the common
   * intake form, this changes its ProgramType to DEFAULT, creating a new draft to do so if
   * necessary.
   */
  private void clearCommonIntakeForm() {
    Optional<ProgramDefinition> maybeCommonIntakeForm = getCommonIntakeForm();
    if (!maybeCommonIntakeForm.isPresent()) {
      return;
    }
    ProgramDefinition draftCommonIntakeProgramDefinition =
        programRepository
            .createOrUpdateDraft(maybeCommonIntakeForm.get().toProgram())
            .getProgramDefinition();
    Program commonIntakeProgram =
        draftCommonIntakeProgramDefinition.toBuilder()
            .setProgramType(ProgramType.DEFAULT)
            .build()
            .toProgram();
    programRepository.updateProgramSync(commonIntakeProgram);
  }

  @Override
  public ImmutableSet<CiviFormError> validateProgramDataForUpdate(
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode,
      ImmutableList<Long> tiGroups) {
    return validateProgramData(
        adminDescription, displayName, displayDescription, externalLink, displayMode, tiGroups);
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

  private ImmutableSet<CiviFormError> validateProgramData(
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode,
      List<Long> tiGroups) {
    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();
    if (displayName.isBlank()) {
      errorsBuilder.add(CiviFormError.of(MISSING_DISPLAY_NAME_MSG));
    }
    if (displayDescription.isBlank()) {
      errorsBuilder.add(CiviFormError.of(MISSING_DISPLAY_DESCRIPTION_MSG));
    } else if (displayMode.equals(DisplayMode.SELECT_TI.getValue()) && tiGroups.isEmpty()) {
      errorsBuilder.add(CiviFormError.of(MISSING_TI_ORGS_FOR_THE_DISPLAY_MODE));
    }
    if (displayMode.isBlank()) {
      errorsBuilder.add(CiviFormError.of(MISSING_DISPLAY_MODE_MSG));
    }
    if (adminDescription.isBlank()) {
      errorsBuilder.add(CiviFormError.of(MISSING_ADMIN_DESCRIPTION_MSG));
    }
    if (!isValidAbsoluteLink(externalLink)) {
      errorsBuilder.add(CiviFormError.of(INVALID_PROGRAM_LINK_FORMAT_MSG));
    }

    return errorsBuilder.build();
  }

  // Checks whether a URL would work correctly if an href attribute was set to it.
  // That is, it must start with http:// or https:// so that the href link doesn't
  // treat it as relative to the current URL.
  //
  // We treat blank links as an exception, so that we can default to the program
  // details page if a link isn't provided.
  private boolean isValidAbsoluteLink(String url) {
    return url.isBlank() || url.startsWith("http://") || url.startsWith("https://");
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
            .setLocalizedConfirmationMessage(
                programDefinition
                    .localizedConfirmationMessage()
                    .updateTranslation(locale, localizationUpdate.localizedConfirmationMessage()))
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

  private void validateProgramText(
      ImmutableSet.Builder<CiviFormError> builder, String fieldName, String text) {
    if (text.isBlank()) {
      builder.add(CiviFormError.of("program " + fieldName.trim() + " cannot be blank"));
    }
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
    ImmutableList<StatusDefinitions.Status> currentStatuses =
        program.statusDefinitions().getStatuses();
    ImmutableList<StatusDefinitions.Status> updatedStatuses =
        status.defaultStatus().orElse(false)
            ? unsetDefaultStatus(currentStatuses, Optional.empty())
            : currentStatuses;

    program
        .statusDefinitions()
        .setStatuses(
            ImmutableList.<StatusDefinitions.Status>builder()
                .addAll(updatedStatuses)
                .add(status)
                .build());

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.updateProgramSync(program.toProgram()).getProgramDefinition())
            .toCompletableFuture()
            .join());
  }

  private ImmutableList<StatusDefinitions.Status> unsetDefaultStatus(
      List<StatusDefinitions.Status> statuses, Optional<String> exceptStatusName) {
    return statuses.stream()
        .<StatusDefinitions.Status>map(
            status ->
                exceptStatusName.map(name -> status.matches(name)).orElse(false)
                    ? status
                    : status.toBuilder().setDefaultStatus(Optional.of(false)).build())
        .collect(ImmutableList.toImmutableList());
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
    ImmutableList<StatusDefinitions.Status> updatedStatuses =
        editedStatus.defaultStatus().orElse(false)
            ? unsetDefaultStatus(statusesCopy, Optional.of(editedStatus.statusText()))
            : ImmutableList.copyOf(statusesCopy);

    program.statusDefinitions().setStatuses(updatedStatuses);

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
  public ProgramDefinition setEligibilityIsGating(
      long programId, boolean gating, boolean isNongatedEligibilityFeatureEnabled)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);
    if (!isNongatedEligibilityFeatureEnabled) {
      return programDefinition;
    }
    programDefinition = programDefinition.toBuilder().setEligibilityIsGating(gating).build();
    return programRepository
        .updateProgramSync(programDefinition.toProgram())
        .getProgramDefinition();
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

  private long getNextBlockId(ProgramDefinition programDefinition) {
    return programDefinition.getMaxBlockDefinitionId() + 1;
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
          IllegalPredicateOrderingException, EligibilityNotValidForProgramTypeException {
    ProgramDefinition programDefinition = getProgramDefinition(programId);

    if (programDefinition.isCommonIntakeForm() && eligibility.isPresent()) {
      throw new EligibilityNotValidForProgramTypeException(programDefinition.programType());
    }

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
    } catch (EligibilityNotValidForProgramTypeException e) {
      // Removing eligibility predicates should always be valid.
      throw new RuntimeException(
          "Unexpected error: removing this predicate is not allowed for this ProgramType", e);
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
  public CompletionStage<ImmutableList<ProgramDefinition>> syncQuestionsToProgramDefinitions(
      ImmutableList<ProgramDefinition> programDefinitions) {

    /* TEMP BUG FIX
     * Because some of the programs are not in the active version,
     * and we need to sync the questions for each program to calculate
     * eligibility state, we must sync each program with a version it
     * is associated with. This diverges from previous behavior where
     * we did not need to sync the programs because the contents of their
     * questions was not needed in the index view.
     */
    return CompletableFuture.completedFuture(
        programDefinitions.stream()
            .map(
                programDef -> {
                  Program p = programDef.toProgram();
                  p.refresh();
                  Version v = p.getVersions().stream().findAny().orElseThrow();
                  try {
                    return syncProgramDefinitionQuestions(
                        programDef, questionService.getReadOnlyVersionedQuestionService(v));
                    /* END TEMP BUG FIX */
                  } catch (QuestionNotFoundException e) {
                    throw new RuntimeException(
                        String.format("Question not found for Program %s", programDef.id()), e);
                  }
                })
            .collect(ImmutableList.toImmutableList()));
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
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException {
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

    if (blockDefinition.hasAddressCorrectionEnabledOnDifferentQuestion(questionDefinitionId)) {
      throw new ProgramQuestionDefinitionInvalidException(
          programId, blockDefinitionId, questionDefinitionId);
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
}
