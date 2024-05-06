package services.program;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.LocalizedStrings.DEFAULT_LOCALE;

import auth.ProgramAcls;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import controllers.BadRequestException;
import controllers.admin.ImageDescriptionNotRemovableException;
import forms.BlockForm;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import models.AccountModel;
import models.ApplicationModel;
import models.DisplayMode;
import models.ProgramModel;
import models.VersionModel;
import modules.MainModule;
import org.apache.commons.lang3.StringUtils;
import play.libs.F;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import repository.AccountRepository;
import repository.ProgramRepository;
import repository.SubmittedApplicationFilter;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.IdentifierBasedPaginationSpec;
import services.LocalizedStrings;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.ProgramBlockValidation.AddQuestionResult;
import services.ProgramBlockValidationFactory;
import services.program.predicate.PredicateDefinition;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

/**
 * The service responsible for accessing the Program resource. Admins create programs to represent
 * specific benefits programs that applicants can apply for. Each program consists of a list of
 * sequential {@link BlockDefinition}s that are rendered one per-page for the applicant. A {@link
 * BlockDefinition} contains one or more {@link QuestionDefinition}s defined in the {@link
 * services.question.QuestionService}.
 */
public final class ProgramService {

  private static final String MISSING_DISPLAY_NAME_MSG =
      "A public display name for the program is required";
  private static final String MISSING_DISPLAY_DESCRIPTION_MSG =
      "A public description for the program is required";
  private static final String MISSING_DISPLAY_MODE_MSG =
      "A program visibility option must be selected";
  private static final String MISSING_ADMIN_NAME_MSG = "A program URL is required";
  private static final String INVALID_ADMIN_NAME_MSG =
      "A program URL may only contain lowercase letters, numbers, and dashes";
  private static final String INVALID_PROGRAM_SLUG_MSG =
      "A program URL must contain at least one letter";
  private static final String INVALID_PROGRAM_LINK_FORMAT_MSG =
      "A program link must begin with 'http://' or 'https://'";
  private static final String MISSING_TI_ORGS_FOR_THE_DISPLAY_MODE =
      "One or more TI Org must be selected for program visibility";

  private final ProgramRepository programRepository;
  private final QuestionService questionService;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final AccountRepository accountRepository;
  private final VersionRepository versionRepository;
  private final ProgramBlockValidationFactory programBlockValidationFactory;

  @Inject
  public ProgramService(
      ProgramRepository programRepository,
      QuestionService questionService,
      AccountRepository accountRepository,
      VersionRepository versionRepository,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ProgramBlockValidationFactory programBlockValidationFactory) {
    this.programRepository = checkNotNull(programRepository);
    this.questionService = checkNotNull(questionService);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.accountRepository = checkNotNull(accountRepository);
    this.versionRepository = checkNotNull(versionRepository);
    this.programBlockValidationFactory = checkNotNull(programBlockValidationFactory);
  }

  /** Get the names for all programs. */
  public ImmutableSet<String> getAllProgramNames() {
    return programRepository.getAllProgramNames();
  }

  /** Get the slugs for all programs. */
  public ImmutableSet<String> getAllProgramSlugs() {
    return getAllProgramNames().stream()
        .map(MainModule.SLUGIFIER::slugify)
        .sorted()
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Get the names for active programs. */
  public ImmutableSet<String> getActiveProgramNames() {
    return versionRepository.getProgramNamesForVersion(versionRepository.getActiveVersion());
  }

  /** Get the data object about the programs that are in the active or draft version. */
  public ActiveAndDraftPrograms getActiveAndDraftPrograms() {
    return ActiveAndDraftPrograms.buildFromCurrentVersionsSynced(this, versionRepository);
  }

  /**
   * Get the data object about the programs that are in the active or draft version without the full
   * question definitions attached to the programs.
   */
  public ActiveAndDraftPrograms getAllActiveAndDraftProgramsWithoutQuestionLoad() {
    return ActiveAndDraftPrograms.buildFromCurrentVersionsUnsynced(versionRepository);
  }

  public ActiveAndDraftPrograms getDisabledProgramsWithoutQuestionLoad() {
    return ActiveAndDraftPrograms.buildFromCurrentVersionsUnsyncedDisabledProgram(
        versionRepository);
  }

  public ActiveAndDraftPrograms getInActiveAndDraftProgramsWithoutQuestionLoad() {
    return ActiveAndDraftPrograms.buildFromCurrentVersionsUnsyncedInUseProgram(versionRepository);
  }

  /*
   * Looks at the most recent version of each program and returns the program marked as the
   * common intake form if it exists. The most recent version may be in the draft or active stage.
   */
  public Optional<ProgramDefinition> getCommonIntakeForm() {
    return getActiveAndDraftPrograms().getMostRecentProgramDefinitions().stream()
        .filter(ProgramDefinition::isCommonIntakeForm)
        .findFirst();
  }

  /**
   * Get the full definition for a given program.
   *
   * <p>This method loads question definitions for all block definitions from a version the program
   * is in. If the program contains a question that is not in any versions associated with the
   * program, a `RuntimeException` is thrown caused by an unexpected QuestionNotFoundException.
   *
   * @param id the ID of the program to retrieve
   * @return the {@link ProgramDefinition} for the given ID if it exists
   * @throws ProgramNotFoundException when ID does not correspond to a real Program
   */
  public ProgramDefinition getFullProgramDefinition(long id) throws ProgramNotFoundException {
    try {
      return getFullProgramDefinitionAsync(id).toCompletableFuture().join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof ProgramNotFoundException) {
        throw new ProgramNotFoundException(id);
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the full definition of a given program asynchronously. The ID may correspond to any
   * version.
   *
   * @param id the ID of the program to retrieve
   * @return the {@link ProgramDefinition} for the given ID if it exists, or a
   *     ProgramNotFoundException is thrown when the future completes and ID does not correspond to
   *     a real Program
   */
  public CompletionStage<ProgramDefinition> getFullProgramDefinitionAsync(long id) {
    return programRepository
        .getFullProgramDefinitionFromCache(id)
        .map(CompletableFuture::completedStage)
        .orElse(
            programRepository
                .lookupProgram(id)
                .thenComposeAsync(
                    programMaybe -> {
                      if (programMaybe.isEmpty()) {
                        return CompletableFuture.failedFuture(new ProgramNotFoundException(id));
                      }

                      return syncProgramAssociations(programMaybe.get());
                    },
                    classLoaderExecutionContext.current()));
  }

  /**
   * Get the full definition for a given program from the cache if it is available.
   *
   * <p>If the full program definition does not yet exist in the cache or caching is not enabled, a
   * method is called to sync program associations, which gets the full program definition, and sets
   * the cache if enabled.
   *
   * <p>The cache is set within the {@link #syncProgramAssociations} method, since that method
   * already checks whether the program is not a draft program, and we only want to set the cache if
   * the program has been published.
   */
  public CompletionStage<ProgramDefinition> getFullProgramDefinition(ProgramModel p) {
    return programRepository
        .getFullProgramDefinitionFromCache(p)
        .map(programDef -> CompletableFuture.completedStage(programDef))
        .orElse(syncProgramAssociations(p));
  }

  /**
   * Get the definition of a given program asynchronously. Gets the active version for the slug.
   *
   * @param programSlug the slug of the program to retrieve
   * @return the active {@link ProgramDefinition} for the given slug if it exists, or a {@link
   *     RuntimeException} is thrown when the future completes and slug does not correspond to a
   *     real Program
   */
  public CompletionStage<ProgramDefinition> getActiveFullProgramDefinitionAsync(
      String programSlug) {
    return programRepository
        .getActiveProgramFromSlug(programSlug)
        .thenComposeAsync(this::getFullProgramDefinition, classLoaderExecutionContext.current());
  }

  /**
   * Get the definition of a given program. Gets the draft version for the slug.
   *
   * @param programSlug the slug of the program to retrieve
   * @return the draft {@link ProgramDefinition} for the given slug if it exists, or a {@link
   *     ProgramDraftNotFoundException} is thrown if a draft is not available.
   */
  public ProgramDefinition getDraftFullProgramDefinition(String programSlug)
      throws ProgramDraftNotFoundException {
    ProgramModel draftProgram = programRepository.getDraftProgramFromSlug(programSlug);
    return syncProgramAssociations(draftProgram).toCompletableFuture().join();
  }

  /**
   * Get the program matching programId as well as all other versions of the program (i.e. all
   * programs with the same name).
   */
  public ImmutableList<ProgramDefinition> getAllVersionsFullProgramDefinition(long programId) {
    return programRepository.getAllProgramVersions(programId).stream()
        .map(p -> getFullProgramDefinition(p).toCompletableFuture().join())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Get the program definition with the underlying question data.
   *
   * <p>Any callers of this function syncing program associations for a non-draft version should use
   * {@link #getFullProgramDefinition(ProgramModel)} to first try getting the full program
   * definition from the cache.
   */
  private CompletionStage<ProgramDefinition> syncProgramAssociations(ProgramModel program) {
    VersionModel activeVersion = versionRepository.getActiveVersion();
    VersionModel maxVersionForProgram =
        programRepository.getVersionsForProgram(program).stream()
            .max(Comparator.comparingLong(p -> p.id))
            .orElseThrow();
    // If the max version is greater than the active version, it is a draft
    if (maxVersionForProgram.id > activeVersion.id) {
      // This method makes multiple calls to get questions for the active and
      // draft versions, so we should only call it if we're syncing program
      // associations for a draft program (which means we're in the admin flow).
      return syncProgramDefinitionQuestions(programRepository.getShallowProgramDefinition(program))
          .thenApply(ProgramDefinition::orderBlockDefinitions);
    }

    ProgramDefinition programDefinition =
        syncProgramDefinitionQuestions(
            programRepository.getShallowProgramDefinition(program), maxVersionForProgram);

    // It is safe to set the program definition cache, since we have already checked that it is
    // not a draft program.
    programRepository.setFullProgramDefinitionCache(
        program.id, programDefinition.orderBlockDefinitions());

    return CompletableFuture.completedStage(programDefinition.orderBlockDefinitions());
  }

  /**
   * Create a new program with an empty block.
   *
   * @param adminName a name for this program for internal use by admins - this is immutable once
   *     set
   * @param adminDescription a description of this program for use by admins
   * @param defaultDisplayName the name of this program to display to applicants
   * @param defaultDisplayDescription a description for this program to display to applicants
   * @param defaultConfirmationMessage a custom message to display on the confirmation screen when
   *     the applicant submits their application
   * @param externalLink A link to an external page containing additional program details
   * @param displayMode The display mode for the program
   * @param programType ProgramType for this Program. If this is set to COMMON_INTAKE_FORM and there
   *     is already another active or draft program with {@link
   *     services.program.ProgramType#COMMON_INTAKE_FORM}, that program's ProgramType will be
   *     changed to {@link services.program.ProgramType#DEFAULT}, creating a new draft of it if
   *     necessary.
   * @param isIntakeFormFeatureEnabled whether or not the common intake form feature is enabled.
   * @param eligibilityIsGating true if an applicant must meet all eligibility criteria in order to
   *     submit an application, and false if an application can submit an application even if they
   *     don't meet some/all of the eligibility criteria.
   * @param tiGroups The List of TiOrgs who have visibility to program in SELECT_TI display mode
   * @return the {@link ProgramDefinition} that was created if succeeded, or a set of errors if
   *     failed
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> createProgramDefinition(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription,
      String defaultConfirmationMessage,
      String externalLink,
      String displayMode,
      boolean eligibilityIsGating,
      ProgramType programType,
      Boolean isIntakeFormFeatureEnabled,
      ImmutableList<Long> tiGroups) {
    ImmutableSet<CiviFormError> errors =
        validateProgramDataForCreate(
            adminName,
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
    ProgramModel program =
        new ProgramModel(
            adminName,
            adminDescription,
            defaultDisplayName,
            defaultDisplayDescription,
            defaultConfirmationMessage,
            externalLink,
            displayMode,
            ImmutableList.of(emptyBlock),
            versionRepository.getDraftVersionOrCreate(),
            programType,
            eligibilityIsGating,
            programAcls);

    return ErrorAnd.of(
        programRepository.getShallowProgramDefinition(
            programRepository.insertProgramSync(program)));
  }

  /**
   * Checks if the provided data is valid for a new program. Does not actually create the program.
   *
   * @param adminName a name for this program for internal use by admins - this is immutable once
   *     set
   * @param displayName a name for this program
   * @param displayDescription the description of what the program provides
   * @param externalLink A link to an external page containing additional program details
   * @param displayMode The display mode for the program
   * @param tiGroups The List of TiOrgs who have visibility to program in SELECT_TI display mode
   * @return a set of errors representing any issues with the provided data.
   */
  public ImmutableSet<CiviFormError> validateProgramDataForCreate(
      String adminName,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode,
      ImmutableList<Long> tiGroups) {
    ImmutableSet.Builder<CiviFormError> errorsBuilder = ImmutableSet.builder();
    errorsBuilder.addAll(
        validateProgramData(displayName, displayDescription, externalLink, displayMode, tiGroups));
    if (adminName.isBlank()) {
      errorsBuilder.add(CiviFormError.of(MISSING_ADMIN_NAME_MSG));
    } else if (!MainModule.SLUGIFIER.slugify(adminName).equals(adminName)) {
      errorsBuilder.add(CiviFormError.of(INVALID_ADMIN_NAME_MSG));
    } else if (StringUtils.isNumeric(MainModule.SLUGIFIER.slugify(adminName))) {
      errorsBuilder.add(CiviFormError.of(INVALID_PROGRAM_SLUG_MSG));
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

  /**
   * Update a program's mutable fields: admin description, display name and description for
   * applicants.
   *
   * @param programId the ID of the program to update
   * @param locale the locale for this update - only applies to applicant display name and
   *     description
   * @param adminDescription the description of this program - visible only to admins
   * @param displayName a name for this program
   * @param displayDescription the description of what the program provides
   * @param confirmationMessage a custom message to display on the confirmation screen when the
   *     applicant submits their application
   * @param externalLink A link to an external page containing additional program details
   * @param displayMode The display mode for the program
   * @param eligibilityIsGating true if an applicant must meet all eligibility criteria in order to
   *     submit an application, and false if an application can submit an application even if they
   *     don't meet some/all of the eligibility criteria.
   * @param programType ProgramType for this Program. If this is set to COMMON_INTAKE_FORM and there
   *     is already another active or draft program with {@link ProgramType#COMMON_INTAKE_FORM},
   *     that program's ProgramType will be changed to {@link ProgramType#DEFAULT}, creating a new
   *     draft of it if necessary.
   * @param isIntakeFormFeatureEnabled whether or not the common intake for feature is enabled.
   * @param tiGroups the TI Orgs having visibility to the program for SELECT_TI display_mode
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors if
   *     failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> updateProgramDefinition(
      long programId,
      Locale locale,
      String adminDescription,
      String displayName,
      String displayDescription,
      String confirmationMessage,
      String externalLink,
      String displayMode,
      boolean eligibilityIsGating,
      ProgramType programType,
      Boolean isIntakeFormFeatureEnabled,
      ImmutableList<Long> tiGroups)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
    ImmutableSet<CiviFormError> errors =
        validateProgramDataForUpdate(
            displayName, displayDescription, externalLink, displayMode, tiGroups);
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

    ProgramModel program =
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
            .setEligibilityIsGating(eligibilityIsGating)
            .setAcls(new ProgramAcls(new HashSet<>(tiGroups)))
            .build()
            .toProgram();

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.getShallowProgramDefinition(
                    programRepository.updateProgramSync(program)))
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
    if (locale.equals(DEFAULT_LOCALE) && confirmationMessage.equals("")) {
      newConfirmationMessageTranslations = LocalizedStrings.withEmptyDefault();
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
        programRepository.getShallowProgramDefinition(
            programRepository.createOrUpdateDraft(maybeCommonIntakeForm.get().toProgram()));
    ProgramModel commonIntakeProgram =
        draftCommonIntakeProgramDefinition.toBuilder()
            .setProgramType(ProgramType.DEFAULT)
            .build()
            .toProgram();
    programRepository.updateProgramSync(commonIntakeProgram);
  }

  /**
   * Checks if the provided data would be valid to update an existing program with. Does not
   * actually update any programs.
   *
   * @param displayName a name for this program
   * @param displayDescription the description of what the program provides
   * @param externalLink A link to an external page containing additional program details
   * @param displayMode The display mode for the program
   * @param tiGroups The List of TiOrgs who have visibility to program in SELECT_TI display mode
   */
  public ImmutableSet<CiviFormError> validateProgramDataForUpdate(
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode,
      ImmutableList<Long> tiGroups) {
    return validateProgramData(
        displayName, displayDescription, externalLink, displayMode, tiGroups);
  }

  /** Create a new draft starting from the program specified by `id`. */
  public ProgramDefinition newDraftOf(long id) throws ProgramNotFoundException {
    // Note: It's unclear that we actually want to update an existing draft this way, as it would
    // effectively reset the  draft which is not part of any user flow. Given the interdependency of
    // draft updates this is likely to cause issues as in #2179.
    return programRepository.getShallowProgramDefinition(
        programRepository.createOrUpdateDraft(this.getFullProgramDefinition(id).toProgram()));
  }

  private ImmutableSet<CiviFormError> validateProgramData(
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

  /**
   * Add or update a localization of the program's publicly-visible display name and description.
   *
   * @param programId the ID of the program to update
   * @param locale the {@link Locale} to update
   * @param localizationUpdate the localization update to apply
   * @return the {@link ProgramDefinition} that was successfully updated, or a set of errors if the
   *     update failed
   * @throws ProgramNotFoundException if the programId does not correspond to a valid program
   * @throws OutOfDateStatusesException if the program's status definitions are out of sync with
   *     those in the provided update
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> updateLocalization(
      long programId, Locale locale, LocalizationUpdate localizationUpdate)
      throws ProgramNotFoundException, OutOfDateStatusesException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
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

    ProgramDefinition.Builder newProgram =
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
                programDefinition.statusDefinitions().setStatuses(toUpdateStatusesBuilder.build()));
    updateSummaryImageDescriptionLocalization(
        programDefinition,
        newProgram,
        localizationUpdate.localizedSummaryImageDescription(),
        locale);

    return ErrorAnd.of(
        syncProgramDefinitionQuestions(
                programRepository.getShallowProgramDefinition(
                    programRepository.updateProgramSync(newProgram.build().toProgram())))
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

  /**
   * Get the email addresses to send a notification to - the program admins if there are any, or the
   * global admins if none.
   */
  public ImmutableList<String> getNotificationEmailAddresses(String programName) {
    ImmutableList<String> explicitProgramAdmins =
        programRepository.getProgramAdministrators(programName).stream()
            .map(AccountModel::getEmailAddress)
            .filter(address -> !Strings.isNullOrEmpty(address))
            .collect(ImmutableList.toImmutableList());
    // If there are any program admins, return them.
    if (explicitProgramAdmins.size() > 0) {
      return explicitProgramAdmins;
    }
    // Return all the global admins email addresses.
    return accountRepository.getGlobalAdmins().stream()
        .map(AccountModel::getEmailAddress)
        .filter(address -> !Strings.isNullOrEmpty(address))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Appends a new status available for application reviews.
   *
   * @param programId The program to update.
   * @param status The status that should be appended.
   * @throws ProgramNotFoundException If the specified Program could not be found.
   * @throws DuplicateStatusException If the provided status to already exists in the list of
   *     available statuses for application review.
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> appendStatus(
      long programId, StatusDefinitions.Status status)
      throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = getFullProgramDefinition(programId);
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
                programRepository.getShallowProgramDefinition(
                    programRepository.updateProgramSync(program.toProgram())))
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

  /**
   * Updates an existing status this is available for application reviews.
   *
   * @param programId The program to update.
   * @param toReplaceStatusName The name of the status that should be updated.
   * @param statusReplacer A single argument function that maps the existing status to the value it
   *     should be updated to. The existing status is provided in case the caller might want to
   *     preserve values from the previous status (e.g. localized text).
   * @throws ProgramNotFoundException If the specified Program could not be found.
   * @throws DuplicateStatusException If the updated status already exists in the list of available
   *     statuses for application review.
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> editStatus(
      long programId,
      String toReplaceStatusName,
      Function<StatusDefinitions.Status, StatusDefinitions.Status> statusReplacer)
      throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = getFullProgramDefinition(programId);
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
                programRepository.getShallowProgramDefinition(
                    programRepository.updateProgramSync(program.toProgram())))
            .toCompletableFuture()
            .join());
  }

  /**
   * Removes an existing status from the list of available statuses for application reviews.
   *
   * @param programId The program to update.
   * @param toRemoveStatusName The name of the status that should be removed.
   * @throws ProgramNotFoundException If the specified Program could not be found.
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> deleteStatus(
      long programId, String toRemoveStatusName) throws ProgramNotFoundException {
    ProgramDefinition program = getFullProgramDefinition(programId);
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
                programRepository.getShallowProgramDefinition(
                    programRepository.updateProgramSync(program.toProgram())))
            .toCompletableFuture()
            .join());
  }

  private static ImmutableMap<String, Integer> statusNameToIndexMap(
      ImmutableList<StatusDefinitions.Status> statuses) {
    return IntStream.range(0, statuses.size())
        .boxed()
        .collect(ImmutableMap.toImmutableMap(i -> statuses.get(i).statusText(), i -> i));
  }

  /**
   * Sets what the summary image description should be for the given locale.
   *
   * <p>If the {@code locale} is the default locale and the {@code summaryImageDescription} is empty
   * or blank, then the description for *all* locales will be erased.
   *
   * @throws ImageDescriptionNotRemovableException if the admin tries to remove a description while
   *     they still have an image.
   */
  public ProgramDefinition setSummaryImageDescription(
      long programId, Locale locale, String summaryImageDescription)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

    if (summaryImageDescription.isBlank() && programDefinition.summaryImageFileKey().isPresent()) {
      throw new ImageDescriptionNotRemovableException(
          "Description can't be removed because an image is present. Delete the image before"
              + " deleting the description.");
    }

    Optional<LocalizedStrings> newStrings =
        getUpdatedSummaryImageDescription(programDefinition, locale, summaryImageDescription);
    programDefinition =
        programDefinition.toBuilder().setLocalizedSummaryImageDescription(newStrings).build();
    return programRepository.getShallowProgramDefinition(
        programRepository.updateProgramSync(programDefinition.toProgram()));
  }

  private void updateSummaryImageDescriptionLocalization(
      ProgramDefinition currentProgram,
      ProgramDefinition.Builder newProgram,
      Optional<String> newDescription,
      Locale locale) {
    // Only update the localization if the current program has an image description set.
    if (currentProgram.localizedSummaryImageDescription().isPresent()
        && newDescription.isPresent()) {
      Optional<LocalizedStrings> newDescriptionStrings =
          getUpdatedSummaryImageDescription(currentProgram, locale, newDescription.get());
      newProgram.setLocalizedSummaryImageDescription(newDescriptionStrings);
    }
  }

  private Optional<LocalizedStrings> getUpdatedSummaryImageDescription(
      ProgramDefinition programDefinition, Locale locale, String summaryImageDescription) {
    if (locale.equals(DEFAULT_LOCALE) && summaryImageDescription.isBlank()) {
      // Clear out all associated translations when the admin deletes a description.
      return Optional.empty();
    }

    Optional<LocalizedStrings> currentDescription =
        programDefinition.localizedSummaryImageDescription();
    LocalizedStrings newStrings;
    if (currentDescription.isEmpty()) {
      newStrings = LocalizedStrings.of(locale, summaryImageDescription);
    } else {
      newStrings = currentDescription.get().updateTranslation(locale, summaryImageDescription);
    }
    return Optional.of(newStrings);
  }

  /**
   * Sets a key that can be used to fetch the summary image for the given program from cloud
   * storage.
   */
  public ProgramDefinition setSummaryImageFileKey(long programId, String fileKey)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
    programDefinition =
        programDefinition.toBuilder().setSummaryImageFileKey(Optional.of(fileKey)).build();
    return programRepository.getShallowProgramDefinition(
        programRepository.updateProgramSync(programDefinition.toProgram()));
  }

  /**
   * Removes the key used to fetch the given program's summary image from cloud storage so that
   * there is no longer an image shown for the given program.
   */
  public ProgramDefinition deleteSummaryImageFileKey(long programId)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
    programDefinition =
        programDefinition.toBuilder().setSummaryImageFileKey(Optional.empty()).build();
    return programRepository.getShallowProgramDefinition(
        programRepository.updateProgramSync(programDefinition.toProgram()));
  }

  /**
   * Adds an empty {@link BlockDefinition} to the end of a given program.
   *
   * @param programId the ID of the program to update
   * @return the {@link ProgramBlockAdditionResult} including the updated program and block if it
   *     succeeded, or a set of errors with the unmodified program and no block if failed.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
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

  /**
   * Adds an empty repeated {@link BlockDefinition} to the given program. The block should be added
   * after the last repeated or nested repeated block with the same ancestor. See {@link
   * ProgramDefinition#orderBlockDefinitions()} for more details about block positioning.
   *
   * @param programId the ID of the program to update
   * @param enumeratorBlockId ID of the enumerator block
   * @return a {@link ProgramBlockAdditionResult} including the updated program and block if it
   *     succeeded, or a set of errors with the unmodified program definition and no block if
   *     failed.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when enumeratorBlockId does not correspond to
   *     an enumerator block in the Program.
   */
  public ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addRepeatedBlockToProgram(
      long programId, long enumeratorBlockId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    return addBlockToProgram(programId, Optional.of(enumeratorBlockId));
  }

  private ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addBlockToProgram(
      long programId, Optional<Long> enumeratorBlockId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
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
    ProgramModel program =
        programDefinition.insertBlockDefinitionInTheRightPlace(blockDefinition).toProgram();
    ProgramDefinition updatedProgram =
        syncProgramDefinitionQuestions(
                programRepository.getShallowProgramDefinition(
                    programRepository.updateProgramSync(program)))
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

  /**
   * Move the block definition one position in the direction specified. If the movement is not
   * allowed, then it is not moved.
   *
   * <p>Movement is not allowed if:
   *
   * <ul>
   *   <li>it would move the block past the ends of the list
   *   <li>it would move a repeated block such that it is not contiguous with its enumerator block's
   *       repeated and nested repeated blocks.
   * </ul>
   *
   * @param programId the ID of the program to update
   * @param blockId the ID of the block to move
   * @return the program definition, with the block moved if it is allowed.
   * @throws IllegalPredicateOrderingException if moving this block violates a program predicate
   */
  public ProgramDefinition moveBlock(
      long programId, long blockId, ProgramDefinition.Direction direction)
      throws ProgramNotFoundException, IllegalPredicateOrderingException {
    final ProgramModel program;
    try {
      program = getFullProgramDefinition(programId).moveBlock(blockId, direction).toProgram();
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(
          "Something happened to the program's block while trying to move it", e);
    }
    return syncProgramDefinitionQuestions(
            programRepository.getShallowProgramDefinition(
                programRepository.updateProgramSync(program)))
        .toCompletableFuture()
        .join();
  }

  /**
   * Update a {@link BlockDefinition}'s attributes.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param blockForm a {@link BlockForm} object containing the new attributes for the block
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors with
   *     the unmodified program definition if failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> updateBlock(
      long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
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

  /**
   * Update a {@link BlockDefinition} with a set of questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param programQuestionDefinitions an {@link ImmutableList} of questions for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws IllegalPredicateOrderingException if changing this block's questions invalidates a
   *     program predicate
   */
  public ProgramDefinition setBlockQuestions(
      long programId,
      long blockDefinitionId,
      ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions)
      throws ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setProgramQuestionDefinitions(programQuestionDefinitions)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  /**
   * Update a {@link BlockDefinition} to include additional questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionIds an {@link ImmutableList} of question IDs for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws QuestionNotFoundException when questionIds does not correspond to real Questions.
   * @throws CantAddQuestionToBlockException if one of the questions can't be added to the block.
   */
  public ProgramDefinition addQuestionsToBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws CantAddQuestionToBlockException,
          QuestionNotFoundException,
          ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

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
      AddQuestionResult canAddQuestion =
          programBlockValidationFactory
              .create()
              .canAddQuestion(programDefinition, blockDefinition, question.getQuestionDefinition());
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

  /**
   * Update a {@link BlockDefinition} to remove questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionIds an {@link ImmutableList} of question IDs to be removed from the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws QuestionNotFoundException when questionIds does not correspond to real Questions.
   * @throws IllegalPredicateOrderingException if removing one or more of the questions invalidates
   *     a predicate - that is, there exists a predicate in this program that depends on at least
   *     one question to remove
   */
  public ProgramDefinition removeQuestionsFromBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws QuestionNotFoundException,
          ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

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

  /**
   * Set the visibility {@link PredicateDefinition} for a block. This predicate describes under what
   * conditions the block should be shown-to or hidden-from an applicant filling out the program
   * form.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param predicate the {@link PredicateDefinition} to set, or empty to remove an existing one.
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws IllegalPredicateOrderingException if this predicate cannot be added to this block
   */
  public ProgramDefinition setBlockVisibilityPredicate(
      long programId, long blockDefinitionId, Optional<PredicateDefinition> predicate)
      throws ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setVisibilityPredicate(predicate)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  /**
   * Set the eligibility {@link PredicateDefinition} for a block. This predicate describes under
   * what conditions the application is considered eligible for the program as of the block.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param eligibility the {@link EligibilityDefinition} for continuing the application.
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws IllegalPredicateOrderingException if this predicate cannot be added to this block
   * @throws EligibilityNotValidForProgramTypeException if this predicate cannot be added to this
   *     ProgramType
   */
  public ProgramDefinition setBlockEligibilityDefinition(
      long programId, long blockDefinitionId, Optional<EligibilityDefinition> eligibility)
      throws ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException,
          EligibilityNotValidForProgramTypeException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

    if (programDefinition.isCommonIntakeForm() && eligibility.isPresent()) {
      throw new EligibilityNotValidForProgramTypeException(programDefinition.programType());
    }

    BlockDefinition blockDefinition =
        programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
            .setEligibilityDefinition(eligibility)
            .build();

    return updateProgramDefinitionWithBlockDefinition(programDefinition, blockDefinition);
  }

  /**
   * Remove the visibility {@link PredicateDefinition} for a block.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   */
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

  /**
   * Remove the eligibility {@link PredicateDefinition} for a block.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   */
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

  /**
   * Delete a block from a program if the block ID is present. Otherwise, does nothing.
   *
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramNeedsABlockException when trying to delete the last block of a Program.
   * @throws IllegalPredicateOrderingException if deleting this block invalidates a predicate in
   *     this program
   */
  public ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException,
          ProgramNeedsABlockException,
          IllegalPredicateOrderingException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);

    ImmutableList<BlockDefinition> newBlocks =
        programDefinition.blockDefinitions().stream()
            .filter(block -> block.id() != blockDefinitionId)
            .collect(ImmutableList.toImmutableList());
    if (newBlocks.isEmpty()) {
      throw new ProgramNeedsABlockException(programId);
    }

    return updateProgramDefinitionWithBlockDefinitions(programDefinition, newBlocks);
  }

  /**
   * Sync all {@link QuestionDefinition}s in a list of {@link ProgramDefinition}s asynchronously, by
   * querying for questions then updating each {@link ProgramDefinition}s.
   *
   * @param programDefinitions the list of program definitions that should be updated
   * @return a list of updated {@link ProgramDefinition}s with all of its associated questions if
   *     they exist, or a QuestionNotFoundException is thrown when the future completes and a
   *     question is not found.
   */
  public CompletionStage<ImmutableList<ProgramDefinition>> syncQuestionsToProgramDefinitions(
      ImmutableList<ProgramDefinition> programDefinitions) {

    /* TEMP BUG FIX
     * Because some of the programs are not in the active version,
     * and we need to sync the questions for each program to calculate
     * eligibility state, we must sync each program with a version it
     * is associated with. This diverges from previous behavior where
     * we did not need to sync the programs because the contents of their
     * questions was not needed in the index view. Note: we only have to do this
     * for programs with eligibility conditions if the cache is disabled or the
     * program is not yet in the cache.
     */

    // Create a map of the questionService for each program and version to minimize database calls.
    Map<Long, ReadOnlyQuestionService> versionToQuestionService = new HashMap<>();
    Map<Long, ReadOnlyQuestionService> programToQuestionService = new HashMap<>();

    for (ProgramDefinition programDef : programDefinitions) {
      ProgramModel p = programDef.toProgram();
      p.refresh();
      // We only need to get the question data if the program has eligibility conditions and the
      // program definition is not in the cache.
      if (programDef.hasEligibilityEnabled()
          && !programRepository.getFullProgramDefinitionFromCache(p).isPresent()) {
        VersionModel v =
            programRepository.getVersionsForProgram(p).stream().findAny().orElseThrow();
        ReadOnlyQuestionService questionServiceForVersion = versionToQuestionService.get(v.id);
        if (questionServiceForVersion == null) {
          questionServiceForVersion =
              questionService.getReadOnlyVersionedQuestionService(v, versionRepository);
          versionToQuestionService.put(v.id, questionServiceForVersion);
        }
        programToQuestionService.put(programDef.id(), questionServiceForVersion);
      }
    }

    return CompletableFuture.completedFuture(
        programDefinitions.stream()
            .map(
                programDef -> {
                  if (!programDef.hasEligibilityEnabled()) {
                    return programDef;
                  }
                  long programId = programDef.id();
                  if (programRepository.getFullProgramDefinitionFromCache(programId).isPresent()) {
                    return programRepository.getFullProgramDefinitionFromCache(programId).get();
                  }
                  try {
                    return syncProgramDefinitionQuestions(
                        programDef, programToQuestionService.get(programId));
                    /* END TEMP BUG FIX */
                  } catch (QuestionNotFoundException e) {
                    throw new RuntimeException(
                        String.format("Question not found for Program %s", programDef.id()), e);
                  }
                })
            .collect(ImmutableList.toImmutableList()));
  }

  /**
   * Set a program question definition to optional or required. If the question definition ID is not
   * present in the program's block, then nothing is changed.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionDefinitionId the ID of the question to update
   * @param optional boolean representing whether the question is optional or required
   * @return the updated program definition
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block
   * @throws ProgramQuestionDefinitionNotFoundException when questionDefinitionId does not
   *     correspond to a real question in the block
   */
  public ProgramDefinition setProgramQuestionDefinitionOptionality(
      long programId, long blockDefinitionId, long questionDefinitionId, boolean optional)
      throws ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
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

  /**
   * Set a program question definition to enable address correction.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionDefinitionId the ID of the question to update
   * @param addressCorrectionEnabled boolean representing whether the question has address
   *     correction enabled
   * @return the updated program definition
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block
   * @throws ProgramQuestionDefinitionNotFoundException when questionDefinitionId does not
   *     correspond to a real question in the block
   */
  public ProgramDefinition setProgramQuestionDefinitionAddressCorrectionEnabled(
      long programId,
      long blockDefinitionId,
      long questionDefinitionId,
      boolean addressCorrectionEnabled)
      throws ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException,
          ProgramQuestionDefinitionInvalidException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
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

  /**
   * Set position of a program question within its block. Used to reorder questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionDefinitionId the ID of the question to update
   * @param newPosition Should be between 0 and N-1 where N is number of questions in the block.
   * @return the updated program definition
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block
   * @throws ProgramQuestionDefinitionNotFoundException when questionDefinitionId does not
   *     correspond to a real question in the block
   */
  public ProgramDefinition setProgramQuestionDefinitionPosition(
      long programId, long blockDefinitionId, long questionDefinitionId, int newPosition)
      throws ProgramNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException,
          InvalidQuestionPositionException {
    ProgramDefinition programDefinition = getFullProgramDefinition(programId);
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

  /**
   * Get all the program's submitted applications. Does not include drafts or deleted applications.
   *
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  public ImmutableList<ApplicationModel> getSubmittedProgramApplications(long programId)
      throws ProgramNotFoundException {
    Optional<ProgramModel> programMaybe =
        programRepository.lookupProgram(programId).toCompletableFuture().join();
    if (programMaybe.isEmpty()) {
      throw new ProgramNotFoundException(programId);
    }
    return programMaybe.get().getSubmittedApplications();
  }

  /**
   * Get all submitted applications for this program and all other previous and future versions of
   * it matches the specified filters.
   *
   * @param paginationSpecEither the query supports two types of pagination, F.Either wraps the
   *     pagination spec to use for a given call.
   * @param filters a set of filters to apply to the examined applications.
   */
  public PaginationResult<ApplicationModel> getSubmittedProgramApplicationsAllVersions(
      long programId,
      F.Either<IdentifierBasedPaginationSpec<Long>, PageNumberBasedPaginationSpec>
          paginationSpecEither,
      SubmittedApplicationFilter filters,
      Request request) {
    return programRepository.getApplicationsForAllProgramVersions(
        programId, paginationSpecEither, filters, request);
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
    // Note: This method is also used for non question updates.
    // TODO(#6249) We should have a focused method for that because getReadOnlyQuestionService()
    // makes multiple calls to get question data for the active and draft versions.
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
            classLoaderExecutionContext.current());
  }

  private ProgramDefinition syncProgramDefinitionQuestions(
      ProgramDefinition programDefinition, VersionModel version) {
    try {
      return syncProgramDefinitionQuestions(
          programDefinition,
          questionService.getReadOnlyVersionedQuestionService(version, versionRepository));
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
            programRepository.getShallowProgramDefinition(
                programRepository.updateProgramSync(program.toProgram())))
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
