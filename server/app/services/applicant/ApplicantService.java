package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import controllers.admin.routes;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import models.ApplicantModel;
import models.ApplicationEventModel;
import models.ApplicationModel;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.StoredFileModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import repository.AccountRepository;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
import repository.ProgramRepository;
import repository.StoredFileRepository;
import repository.TimeFilter;
import repository.VersionRepository;
import services.Address;
import services.DeploymentType;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.PhoneValidationResult;
import services.PhoneValidationUtils;
import services.applicant.ApplicantPersonalInfo.ApplicantType;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ApplicationNotEligibleException;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.predicate.JsonPathPredicateGeneratorFactory;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PhoneQuestion;
import services.applicant.question.Scalar;
import services.application.ApplicationEventDetails;
import services.cloud.aws.SimpleEmail;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusionGroup;
import services.geo.esri.EsriClient;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.QuestionType;
import services.question.types.ScalarType;
import views.applicant.AddressCorrectionBlockView;

/**
 * The service responsible for accessing the Applicant resource. Applicants can view program
 * applications defined by the {@link services.program.ProgramService} as a series of {@link
 * Block}s, one per-page. When an applicant submits the form for a Block the ApplicantService is
 * responsible for validating and persisting their answers and then providing the next Block for
 * them to view, if any.
 */
public final class ApplicantService {
  private static final Logger logger = LoggerFactory.getLogger(ApplicantService.class);

  private final ApplicationEventRepository applicationEventRepository;
  private final ApplicationRepository applicationRepository;
  private final AccountRepository accountRepository;
  private final StoredFileRepository storedFileRepository;
  private final JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory;
  private final VersionRepository versionRepository;
  private final ProgramRepository programRepository;
  private final ProgramService programService;
  private final SimpleEmail amazonSESClient;
  private final Clock clock;
  private final String baseUrl;
  private final boolean isStaging;
  private final HttpExecutionContext httpExecutionContext;
  private final String stagingProgramAdminNotificationMailingList;
  private final String stagingTiNotificationMailingList;
  private final String stagingApplicantNotificationMailingList;
  private final ServiceAreaUpdateResolver serviceAreaUpdateResolver;
  private final EsriClient esriClient;
  private final MessagesApi messagesApi;

  @Inject
  public ApplicantService(
      ApplicationEventRepository applicationEventRepository,
      ApplicationRepository applicationRepository,
      AccountRepository accountRepository,
      VersionRepository versionRepository,
      ProgramRepository programRepository,
      StoredFileRepository storedFileRepository,
      JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory,
      ProgramService programService,
      SimpleEmail amazonSESClient,
      Clock clock,
      Config configuration,
      HttpExecutionContext httpExecutionContext,
      DeploymentType deploymentType,
      ServiceAreaUpdateResolver serviceAreaUpdateResolver,
      EsriClient esriClient,
      MessagesApi messagesApi) {
    this.applicationEventRepository = checkNotNull(applicationEventRepository);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.accountRepository = checkNotNull(accountRepository);
    this.versionRepository = checkNotNull(versionRepository);
    this.programRepository = checkNotNull(programRepository);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.jsonPathPredicateGeneratorFactory = checkNotNull(jsonPathPredicateGeneratorFactory);
    this.programService = checkNotNull(programService);
    this.amazonSESClient = checkNotNull(amazonSESClient);
    this.clock = checkNotNull(clock);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.serviceAreaUpdateResolver = checkNotNull(serviceAreaUpdateResolver);
    this.messagesApi = checkNotNull(messagesApi);

    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.isStaging = checkNotNull(deploymentType).isStaging();
    this.stagingProgramAdminNotificationMailingList =
        checkNotNull(configuration).getString("staging_program_admin_notification_mailing_list");
    this.stagingTiNotificationMailingList =
        checkNotNull(configuration).getString("staging_ti_notification_mailing_list");
    this.stagingApplicantNotificationMailingList =
        checkNotNull(configuration).getString("staging_applicant_notification_mailing_list");
    this.esriClient = checkNotNull(esriClient);
  }

  /** Create a new {@link ApplicantModel}. */
  public CompletionStage<ApplicantModel> createApplicant() {

    ApplicantModel applicant = new ApplicantModel();
    return accountRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
  }

  /**
   * Get a {@link ReadOnlyApplicantProgramService} which implements synchronous, in-memory read
   * behavior relevant to an applicant for a specific program.
   *
   * <p>A ProgramNotFoundException may be thrown when the future completes if the programId does not
   * correspond to a real Program.
   */
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId) {
    CompletableFuture<Optional<ApplicantModel>> applicantCompletableFuture =
        accountRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getFullProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              ApplicantModel applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();

              return new ReadOnlyApplicantProgramServiceImpl(
                  jsonPathPredicateGeneratorFactory,
                  applicant.getApplicantData(),
                  programDefinition,
                  baseUrl);
            },
            httpExecutionContext.current());
  }

  /** Get a {@link ReadOnlyApplicantProgramService} from an application. */
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      ApplicationModel application) {
    try {
      return CompletableFuture.completedFuture(
          new ReadOnlyApplicantProgramServiceImpl(
              jsonPathPredicateGeneratorFactory,
              application.getApplicantData(),
              programService.getFullProgramDefinition(application.getProgram().id),
              baseUrl));
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException("Cannot find a program that has applications for it.", e);
    }
  }

  /** Get a {@link ReadOnlyApplicantProgramService} from an application and program definition. */
  public ReadOnlyApplicantProgramService getReadOnlyApplicantProgramService(
      ApplicationModel application, ProgramDefinition programDefinition) {
    return new ReadOnlyApplicantProgramServiceImpl(
        jsonPathPredicateGeneratorFactory,
        application.getApplicantData(),
        programDefinition,
        baseUrl);
  }

  /** Get a {@link ReadOnlyApplicantProgramService} from applicant data and a program definition. */
  private ReadOnlyApplicantProgramService getReadOnlyApplicantProgramService(
      ApplicantData applicantData, ProgramDefinition programDefinition) {
    return new ReadOnlyApplicantProgramServiceImpl(
        jsonPathPredicateGeneratorFactory, applicantData, programDefinition, baseUrl);
  }

  /**
   * Attempt to perform a set of updates to the applicant's {@link ApplicantData}. If updates are
   * valid, they are saved to storage. If not, a set of errors are returned along with the modified
   * {@link ApplicantData}, but none of the updates are persisted to storage.
   *
   * <p>Updates are atomic i.e. if any of them fail validation, none of them will be written.
   *
   * @return a {@link ReadOnlyApplicantProgramService} that reflects the updates. If the service
   *     cannot perform the update due to exceptions, they are wrapped in `CompletionException`s.
   *     <p>Below list all possible exceptions:
   *     <p>
   *     <ul>
   *       <li>`ApplicantNotFoundException` - Invalid applicantId is given.
   *       <li>`IllegalArgumentException` - Invalid paths that collide with reserved keys.
   *       <li>`PathNotInBlockException` - Specified paths do not point to any scalars defined in
   *           the block.
   *       <li>`ProgramBlockNotFoundException` - Invalid combination of programId and blockId is
   *           given.
   *       <li>`ProgramNotFoundException` - Specified programId does not correspond to a real
   *           Program.
   *       <li>`UnsupportedScalarTypeException` - Specified paths point to an unsupported type of
   *           scalar.
   *     </ul>
   */
  public CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      long applicantId,
      long programId,
      String blockId,
      ImmutableMap<String, String> updateMap,
      boolean addressServiceAreaValidationEnabled) {
    ImmutableSet<Update> updates =
        updateMap.entrySet().stream()
            .map(entry -> Update.create(Path.create(entry.getKey()), entry.getValue()))
            .collect(ImmutableSet.toImmutableSet());

    // Ensures updates do not collide with metadata scalars. "keyName[]" collides with "keyName".
    boolean updatePathsContainReservedKeys =
        updates.stream()
            .map(Update::path)
            .map(path -> path.isArrayElement() ? path.withoutArrayReference() : path)
            .anyMatch(path -> Scalar.getMetadataScalarKeys().contains(path.keyName()));
    if (updatePathsContainReservedKeys) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Path contained reserved scalar key"));
    }

    return stageAndUpdateIfValid(
        applicantId, programId, blockId, updateMap, updates, addressServiceAreaValidationEnabled);
  }

  private CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      long applicantId,
      long programId,
      String blockId,
      ImmutableMap<String, String> updateMap,
      ImmutableSet<Update> updates,
      boolean addressServiceAreaValidationEnabled) {
    CompletableFuture<Optional<ApplicantModel>> applicantCompletableFuture =
        accountRepository.lookupApplicant(applicantId).toCompletableFuture();

    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getFullProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenComposeAsync(
            (v) -> {
              Optional<ApplicantModel> applicantMaybe = applicantCompletableFuture.join();
              if (applicantMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(new ApplicantNotFoundException(applicantId));
              }
              ApplicantModel applicant = applicantMaybe.get();

              // Create a ReadOnlyApplicantProgramService and get the current block.
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();
              ReadOnlyApplicantProgramService readOnlyApplicantProgramServiceBeforeUpdate =
                  new ReadOnlyApplicantProgramServiceImpl(
                      jsonPathPredicateGeneratorFactory,
                      applicant.getApplicantData(),
                      programDefinition,
                      baseUrl);
              Optional<Block> maybeBlockBeforeUpdate =
                  readOnlyApplicantProgramServiceBeforeUpdate.getActiveBlock(blockId);
              if (maybeBlockBeforeUpdate.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ProgramBlockNotFoundException(programId, blockId));
              }
              Block blockBeforeUpdate = maybeBlockBeforeUpdate.get();

              if (addressServiceAreaValidationEnabled
                  && blockBeforeUpdate.getLeafAddressNodeServiceAreaIds().isPresent()) {
                return serviceAreaUpdateResolver
                    .getServiceAreaUpdate(blockBeforeUpdate, updateMap)
                    .thenComposeAsync(
                        (serviceAreaUpdate) -> {
                          return stageAndUpdateIfValid(
                              applicant,
                              baseUrl,
                              blockBeforeUpdate,
                              programDefinition,
                              updates,
                              serviceAreaUpdate);
                        },
                        httpExecutionContext.current());
              }

              return stageAndUpdateIfValid(
                  applicant,
                  baseUrl,
                  blockBeforeUpdate,
                  programDefinition,
                  updates,
                  Optional.empty());
            },
            httpExecutionContext.current())
        .thenCompose(
            (v) ->
                applicationRepository
                    .createOrUpdateDraft(applicantId, programId)
                    .thenApplyAsync(appDraft -> v));
  }

  private CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      ApplicantModel applicant,
      String baseUrl,
      Block blockBeforeUpdate,
      ProgramDefinition programDefinition,
      ImmutableSet<Update> updates,
      Optional<ServiceAreaUpdate> serviceAreaUpdate) {
    UpdateMetadata updateMetadata = UpdateMetadata.create(programDefinition.id(), clock.millis());
    ImmutableMap<Path, String> failedUpdates;
    try {
      failedUpdates =
          stageUpdates(
              applicant.getApplicantData(),
              blockBeforeUpdate,
              updateMetadata,
              updates,
              serviceAreaUpdate);
    } catch (UnsupportedScalarTypeException | PathNotInBlockException e) {
      return CompletableFuture.failedFuture(e);
    }

    ReadOnlyApplicantProgramService roApplicantProgramService =
        new ReadOnlyApplicantProgramServiceImpl(
            jsonPathPredicateGeneratorFactory,
            applicant.getApplicantData(),
            programDefinition,
            baseUrl,
            failedUpdates);

    Optional<Block> blockMaybe =
        roApplicantProgramService.getActiveBlock(blockBeforeUpdate.getId());
    if (blockMaybe.isPresent() && !blockMaybe.get().hasErrors()) {
      return accountRepository
          .updateApplicant(applicant)
          .thenApplyAsync(
              (finishedSaving) -> roApplicantProgramService, httpExecutionContext.current());
    }

    return CompletableFuture.completedFuture(roApplicantProgramService);
  }

  /**
   * Create a new active {@link ApplicationModel} for the applicant applying to the program.
   *
   * <p>An application is a snapshot of all the answers the applicant has filled in so far, along
   * with association with the applicant and a program that the applicant is applying to.
   *
   * @param submitterProfile the user that submitted the application, if it is a TI the application
   *     is associated with this profile too.
   * @return the saved {@link ApplicationModel}. If the submission failed, a {@link
   *     ApplicationSubmissionException} is thrown and wrapped in a `CompletionException`.
   */
  public CompletionStage<ApplicationModel> submitApplication(
      long applicantId, long programId, CiviFormProfile submitterProfile) {
    if (submitterProfile.isTrustedIntermediary()) {
      return getReadOnlyApplicantProgramService(applicantId, programId)
          .thenCompose(ro -> validateApplicationForSubmission(ro, programId))
          .thenComposeAsync(v -> submitterProfile.getAccount(), httpExecutionContext.current())
          .thenComposeAsync(
              account ->
                  submitApplication(
                      applicantId,
                      programId,
                      /* tiSubmitterEmail= */ Optional.of(account.getEmailAddress())),
              httpExecutionContext.current());
    }

    return getReadOnlyApplicantProgramService(applicantId, programId)
        .thenCompose(ro -> validateApplicationForSubmission(ro, programId))
        .thenCompose(
            v ->
                submitApplication(
                    applicantId, programId, /* tiSubmitterEmail= */ Optional.empty()));
  }

  /**
   * Set the status for the newly submitted application to the given status.
   *
   * <p>Because this is done programmatically, we insert an event without an account attached to it.
   * This should only be used when setting the application status to the default status for the
   * program.
   *
   * @param application the application on which to set the status
   * @param status the status to set the application to
   */
  private CompletionStage<ApplicationEventModel> setApplicationStatus(
      ApplicationModel application, StatusDefinitions.Status status) {
    // Set the status for the application automatically to the default status
    ApplicationEventDetails.StatusEvent statusEvent =
        ApplicationEventDetails.StatusEvent.builder()
            .setStatusText(status.statusText())
            .setEmailSent(true)
            .build();
    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(statusEvent)
            .build();
    // Because we are doing this automatically, set the Account to empty.
    return applicationEventRepository.insertAsync(
        new ApplicationEventModel(application, /* creator= */ Optional.empty(), details));
  }

  @VisibleForTesting
  CompletionStage<ApplicationModel> submitApplication(
      long applicantId, long programId, Optional<String> tiSubmitterEmail) {
    CompletableFuture<ApplicantPersonalInfo> applicantLabelFuture =
        getPersonalInfo(applicantId).toCompletableFuture();
    CompletableFuture<Optional<ApplicationModel>> applicationFuture =
        applicationRepository
            .submitApplication(applicantId, programId, tiSubmitterEmail)
            .toCompletableFuture();
    return CompletableFuture.allOf(applicantLabelFuture, applicationFuture)
        .thenComposeAsync(
            (v) -> {
              Optional<ApplicationModel> applicationMaybe = applicationFuture.join();
              if (applicationMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ApplicationSubmissionException(applicantId, programId));
              }

              ApplicationModel application = applicationMaybe.get();
              ProgramModel applicationProgram = application.getProgram();
              ProgramDefinition programDefinition =
                  programRepository.getProgramDefinition(applicationProgram);
              String programName = programDefinition.adminName();
              Optional<StatusDefinitions.Status> maybeDefaultStatus =
                  applicationProgram.getDefaultStatus();

              CompletableFuture<ApplicationEventModel> updateStatusFuture =
                  maybeDefaultStatus
                      .map(
                          status -> setApplicationStatus(application, status).toCompletableFuture())
                      .orElse(CompletableFuture.completedFuture(null));

              CompletableFuture<Void> notifyProgramAdminsFuture =
                  CompletableFuture.runAsync(
                      () ->
                          notifyProgramAdmins(applicantId, programId, application.id, programName));

              CompletableFuture<Void> notifyTiSubmitterFuture =
                  tiSubmitterEmail
                      .map(
                          email ->
                              notifyTiSubmitter(
                                      email,
                                      applicantId,
                                      application.id,
                                      programName,
                                      maybeDefaultStatus)
                                  .toCompletableFuture())
                      .orElse(CompletableFuture.completedFuture(null));

              ApplicantPersonalInfo applicantPersonalInfo = applicantLabelFuture.join();
              Optional<String> applicantEmail =
                  applicantPersonalInfo.getType() == ApplicantType.LOGGED_IN
                      ? applicantPersonalInfo.loggedIn().email()
                      : Optional.empty();

              CompletableFuture<Void> notifyApplicantFuture =
                  applicantEmail
                      .map(
                          email ->
                              notifyApplicant(
                                      applicantId,
                                      application.id,
                                      email,
                                      programDefinition,
                                      maybeDefaultStatus)
                                  .toCompletableFuture())
                      .orElse(CompletableFuture.completedFuture(null));

              return CompletableFuture.allOf(
                      updateStatusFuture,
                      notifyProgramAdminsFuture,
                      notifyApplicantFuture,
                      notifyTiSubmitterFuture,
                      updateStoredFileAclsForSubmit(applicantId, programId).toCompletableFuture())
                  .thenApplyAsync((ignoreVoid) -> application, httpExecutionContext.current());
            },
            httpExecutionContext.current());
  }

  /**
   * Validates that the application is complete and correct to submit.
   *
   * <p>An application may be submitted but incomplete if the application view with submit button
   * contains stale data that has changed visibility conditions.
   *
   * @return a {@link ApplicationOutOfDateException} or {@link ApplicationNotEligibleException}
   *     wrapped in a failed future with a user visible message for the issue.
   */
  private CompletableFuture<Void> validateApplicationForSubmission(
      ReadOnlyApplicantProgramService roApplicantProgramService, long programId) {
    // Check that all blocks have been answered.
    if (!roApplicantProgramService.getFirstIncompleteBlockExcludingStatic().isEmpty()) {
      throw new ApplicationOutOfDateException();
    }

    try {
      if (!programService.getFullProgramDefinition(programId).eligibilityIsGating()) {
        return CompletableFuture.completedFuture(null);
      }
    } catch (ProgramNotFoundException e) {
      return CompletableFuture.completedFuture(null);
    }

    if (!roApplicantProgramService.isApplicationEligible()) {
      throw new ApplicationNotEligibleException();
    }

    return CompletableFuture.completedFuture(null);
  }

  /**
   * When an application is submitted, we store the name of its program in the ACLs for each file in
   * the application.
   */
  private CompletionStage<Void> updateStoredFileAclsForSubmit(long applicantId, long programId) {
    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getFullProgramDefinitionAsync(programId).toCompletableFuture();

    CompletableFuture<List<StoredFileModel>> storedFilesFuture =
        getReadOnlyApplicantProgramService(applicantId, programId)
            .thenApplyAsync(
                ReadOnlyApplicantProgramService::getStoredFileKeys, httpExecutionContext.current())
            .thenComposeAsync(storedFileRepository::lookupFiles, httpExecutionContext.current())
            .toCompletableFuture();

    return CompletableFuture.allOf(programDefinitionCompletableFuture, storedFilesFuture)
        .thenComposeAsync(
            (ignoreVoid) -> {
              List<StoredFileModel> storedFiles = storedFilesFuture.join();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();
              CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

              for (StoredFileModel file : storedFiles) {
                file.getAcls().addProgramToReaders(programDefinition);
                future =
                    CompletableFuture.allOf(
                        future, storedFileRepository.update(file).toCompletableFuture());
              }

              return future;
            },
            httpExecutionContext.current());
  }

  /**
   * Email the program admins to inform them an application has been submitted.
   *
   * @param applicantId the ID of the applicant
   * @param programId the ID of the program
   * @param applicationId the ID of the application
   * @param programName the name of the program that was applied to
   */
  private void notifyProgramAdmins(
      long applicantId, long programId, long applicationId, String programName) {
    String applicationViewLink =
        controllers.admin.routes.AdminApplicationController.show(programId, applicationId).url();

    String viewLink =
        baseUrl
            + routes.AdminApplicationController.index(
                    programId,
                    /* search= */ Optional.empty(),
                    /* page= */ Optional.empty(),
                    /* fromDate= */ Optional.empty(),
                    /* untilDate= */ Optional.empty(),
                    /* applicationStatus= */ Optional.empty(),
                    Optional.of(applicationViewLink))
                .url();

    String subject = String.format("New application %d submitted", applicationId);
    String message =
        String.format(
            "Applicant %d submitted a new application %d to program %s.\n"
                + "View the application at %s.",
            applicantId, applicationId, programName, viewLink);
    if (isStaging) {
      amazonSESClient.send(stagingProgramAdminNotificationMailingList, subject, message);
    } else {
      amazonSESClient.send(
          programService.getNotificationEmailAddresses(programName), subject, message);
    }
  }

  /**
   * Email the Trusted Intermediary with either the default application email or the status' defined
   * email. Will send the default application email if the status does not have an email body
   * defined.
   *
   * @param tiEmail the email address of the Trusted Intermediary
   * @param applicantId the ID of the applicant
   * @param applicationId the ID of the application
   * @param programName the name of the program that was applied to
   * @param status the status from which to get the email body to send. Empty if no default status
   *     is set for the program.
   */
  private CompletionStage<Void> notifyTiSubmitter(
      String tiEmail,
      long applicantId,
      long applicationId,
      String programName,
      Optional<StatusDefinitions.Status> status) {
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dayQuery= */ Optional.empty(),
                    /* monthQuery= */ Optional.empty(),
                    /* yearQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    CompletableFuture<Optional<Locale>> localeFuture =
        getPreferredTiLocale(tiEmail).toCompletableFuture();
    return localeFuture
        .thenAcceptAsync(
            (localeMaybe) -> {
              // Not blocking since it already completed
              Locale locale = localeMaybe.orElse(LocalizedStrings.DEFAULT_LOCALE);
              Messages messages =
                  messagesApi.preferred(ImmutableSet.of(Lang.forCode(locale.toLanguageTag())));
              String subject =
                  messages.at(
                      MessageKey.EMAIL_TI_APPLICATION_SUBMITTED_SUBJECT.getKeyName(),
                      programName,
                      applicantId);
              boolean useStatusMessage =
                  status.map(s -> s.localizedEmailBodyText().isPresent()).orElse(false);
              String message =
                  String.format(
                      "%s\n%s",
                      useStatusMessage
                          ? status.get().localizedEmailBodyText().get().getOrDefault(locale)
                          : messages.at(
                              MessageKey.EMAIL_TI_APPLICATION_SUBMITTED_BODY.getKeyName(),
                              programName,
                              applicantId,
                              applicationId),
                      messages.at(
                          MessageKey.EMAIL_TI_MANAGE_YOUR_CLIENTS.getKeyName(), tiDashLink));
              if (isStaging) {
                amazonSESClient.send(stagingTiNotificationMailingList, subject, message);
              } else {
                amazonSESClient.send(tiEmail, subject, message);
              }
            })
        .toCompletableFuture();
  }

  /**
   * Email the applicant with either the default application email or the status' defined email.
   * Will send the default application email if the status does not have an email body defined.
   *
   * @param applicantId the ID of the applicant
   * @param applicationId the ID of the application
   * @param applicantEmail the applicant's email address
   * @param programDef the ProgramDefinition that the applicant applied for
   * @param status the status from which to get the email body to send. Empty if no default status
   *     is set for the program.
   */
  private CompletionStage<Void> notifyApplicant(
      long applicantId,
      long applicationId,
      String applicantEmail,
      ProgramDefinition programDef,
      Optional<StatusDefinitions.Status> status) {
    CompletableFuture<Optional<Locale>> localeFuture =
        getPreferredLocale(applicantId).toCompletableFuture();
    return localeFuture.thenAcceptAsync(
        (localeMaybe) -> {
          Locale locale = localeMaybe.orElse(LocalizedStrings.DEFAULT_LOCALE);
          boolean useStatusMessage =
              status.map(s -> s.localizedEmailBodyText().isPresent()).orElse(false);
          Messages messages =
              messagesApi.preferred(ImmutableSet.of(Lang.forCode(locale.toLanguageTag())));
          String programName = programDef.localizedName().getOrDefault(locale);
          String subject =
              messages.at(MessageKey.EMAIL_APPLICATION_RECEIVED_SUBJECT.getKeyName(), programName);
          String message =
              String.format(
                  "%s\n%s",
                  useStatusMessage
                      ? status.get().localizedEmailBodyText().get().getOrDefault(locale)
                      : messages.at(
                          MessageKey.EMAIL_APPLICATION_RECEIVED_BODY.getKeyName(),
                          programName,
                          applicantId,
                          applicationId),
                  messages.at(MessageKey.EMAIL_LOGIN_TO_CIVIFORM.getKeyName(), baseUrl));
          if (isStaging) {
            amazonSESClient.send(stagingApplicantNotificationMailingList, subject, message);
          } else {
            amazonSESClient.send(applicantEmail, subject, message);
          }
        },
        httpExecutionContext.current());
  }

  /**
   * Returns an ApplicantPersonalInfo, which represents some contact/display info for an applicant.
   */
  public CompletionStage<ApplicantPersonalInfo> getPersonalInfo(long applicantId) {
    return accountRepository
        .lookupApplicant(applicantId)
        .thenApplyAsync(
            applicant -> {
              boolean hasAuthorityId =
                  applicant.isPresent()
                      && !Strings.isNullOrEmpty(applicant.get().getAccount().getAuthorityId());
              boolean isManagedByTi =
                  applicant.isPresent()
                      && applicant.get().getAccount().getManagedByGroup().isPresent();

              if (!hasAuthorityId && !isManagedByTi) {
                // The authority ID is the source of truth for whether a user is logged in. However,
                // if they were created by a TI, we skip this return and return later on with a more
                // specific oneof value.
                return ApplicantPersonalInfo.ofGuestUser();
              }

              Representation.Builder builder = Representation.builder();

              Optional<String> name = applicant.get().getApplicantData().getApplicantName();
              if (name.isPresent() && !Strings.isNullOrEmpty(name.get())) {
                builder.setName(name.get());
              }
              String emailAddress = applicant.get().getAccount().getEmailAddress();
              if (!Strings.isNullOrEmpty(emailAddress)) {
                builder.setEmail(emailAddress);
              }

              if (hasAuthorityId) {
                return ApplicantPersonalInfo.ofLoggedInUser(builder.build());
              } else {
                return ApplicantPersonalInfo.ofTiPartiallyCreated(builder.build());
              }
            },
            httpExecutionContext.current());
  }

  /** Return the preferred locale of the given applicant id. */
  public CompletionStage<Optional<Locale>> getPreferredLocale(long applicantId) {
    return accountRepository
        .lookupApplicant(applicantId)
        .thenApplyAsync(
            applicant ->
                applicant.map(ApplicantModel::getApplicantData).map(ApplicantData::preferredLocale),
            httpExecutionContext.current());
  }

  /** Return the preferred locale of the given TI email. */
  public CompletionStage<Optional<Locale>> getPreferredTiLocale(String tiEmail) {
    return accountRepository
        .lookupAccountByEmailAsync(tiEmail)
        .thenApplyAsync(
            account -> {
              if (account.isEmpty()) {
                return Optional.empty();
              }
              // There's really only one applicant per account. See notes in Account.java.
              Optional<ApplicantModel> applicant = account.get().newestApplicant();
              return applicant
                  .map(ApplicantModel::getApplicantData)
                  .map(ApplicantData::preferredLocale);
            },
            httpExecutionContext.current());
  }

  /**
   * Return a filtered set of applications, including applications from previous versions, with
   * program, applicant, and account associations eager loaded. Results are ordered by application
   * ID in ascending order.
   */
  public ImmutableList<ApplicationModel> getApplications(TimeFilter submitTimeFilter) {
    return applicationRepository.getApplications(submitTimeFilter);
  }

  /**
   * Return all programs that are appropriate to serve to an applicant. Appropriate programs are
   * those where the applicant:
   *
   * <ul>
   *   <li>Has a draft application
   *   <li>Has previously applied
   *   <li>Any other programs that are public
   * </ul>
   */
  public CompletionStage<ApplicationPrograms> relevantProgramsForApplicant(
      long applicantId, CiviFormProfile requesterProfile) {
    // Note: The Program model associated with the application is eagerly loaded.
    CompletableFuture<ImmutableSet<ApplicationModel>> applicationsFuture =
        applicationRepository
            .getApplicationsForApplicant(
                applicantId, ImmutableSet.of(LifecycleStage.DRAFT, LifecycleStage.ACTIVE))
            .toCompletableFuture();
    ImmutableList<ProgramDefinition> activeProgramDefinitions =
        versionRepository.getProgramsForVersion(versionRepository.getActiveVersion()).stream()
            .map(p -> programRepository.getProgramDefinition(p))
            .filter(
                pdef ->
                    pdef.displayMode().equals(DisplayMode.PUBLIC)
                        || (requesterProfile.isTrustedIntermediary()
                            && pdef.displayMode().equals(DisplayMode.TI_ONLY))
                        || (pdef.displayMode().equals(DisplayMode.SELECT_TI)
                            && pdef.acls().hasProgramViewPermission(requesterProfile)))
            .collect(ImmutableList.toImmutableList());

    return applicationsFuture
        .thenComposeAsync(
            v -> {
              ImmutableSet<ApplicationModel> applications = applicationsFuture.join();
              if (applications.isEmpty()) {
                return CompletableFuture.completedFuture(activeProgramDefinitions);
              }
              List<ProgramDefinition> programDefinitionsList =
                  applications.stream()
                      .map(
                          application ->
                              programRepository.getProgramDefinition(application.getProgram()))
                      .collect(Collectors.toList());
              programDefinitionsList.addAll(activeProgramDefinitions);
              return programService.syncQuestionsToProgramDefinitions(
                  programDefinitionsList.stream().collect(ImmutableList.toImmutableList()));
            })
        .thenApplyAsync(
            allPrograms -> {
              ImmutableSet<ApplicationModel> applications = applicationsFuture.join();
              logDuplicateDrafts(applications);
              return relevantProgramsForApplicantInternal(
                  activeProgramDefinitions, applications, allPrograms);
            },
            httpExecutionContext.current());
  }

  /**
   * Find programs the applicant may be eligible for, if they've started an application.
   *
   * <p>If no application has been started all programs are returned because their eligibility
   * status is not set by relevantProgramsForApplicant().
   *
   * @return All unsubmitted programs that are appropriate to serve to an applicant and that they
   *     may be eligible for. Includes programs with matching eligibility criteria or no eligibility
   *     criteria.
   *     <p>Does not include the Common Intake Form.
   *     <p>"Appropriate programs" those returned by {@link #relevantProgramsForApplicant(long,
   *     auth.CiviFormProfile)}.
   */
  public CompletionStage<ImmutableList<ApplicantProgramData>> maybeEligibleProgramsForApplicant(
      long applicantId, CiviFormProfile requesterProfile) {
    return relevantProgramsForApplicant(applicantId, requesterProfile)
        .thenApplyAsync(
            relevantPrograms ->
                Stream.of(
                        relevantPrograms.inProgress(),
                        relevantPrograms.unapplied(),
                        relevantPrograms.submitted())
                    .flatMap(ImmutableList::stream)
                    // Return all programs the user is eligible for, or that have no
                    // eligibility conditions.
                    .filter(programData -> programData.isProgramMaybeEligible().orElse(true))
                    .filter(programData -> !programData.program().isCommonIntakeForm())
                    .collect(ImmutableList.toImmutableList()),
            httpExecutionContext.current());
  }

  /**
   * Returns whether an applicant is maybe eligible for a program based on their latest answers, and
   * empty if there are no eligibility conditions for the program.
   */
  public Optional<Boolean> getApplicantMayBeEligibleStatus(
      ApplicantModel applicant, ProgramDefinition programDefinition) {
    ReadOnlyApplicantProgramService roAppProgramService =
        getReadOnlyApplicantProgramService(applicant.getApplicantData(), programDefinition);
    return programDefinition.hasEligibilityEnabled()
        ? Optional.of(!roAppProgramService.isApplicationNotEligible())
        : Optional.empty();
  }

  /**
   * Returns whether or not an application is eligible for a program. This uses the answers at the
   * time of the application rather than the applicant's latest answer to a question. Returns empty
   * if there are no eligibility conditions for the program.
   */
  public Optional<Boolean> getApplicationEligibilityStatus(
      ApplicationModel application, ProgramDefinition programDefinition) {
    ReadOnlyApplicantProgramService roAppProgramService =
        getReadOnlyApplicantProgramService(application, programDefinition);
    return programDefinition.hasEligibilityEnabled()
        ? Optional.of(!roAppProgramService.isApplicationNotEligible())
        : Optional.empty();
  }

  private ApplicationPrograms relevantProgramsForApplicantInternal(
      ImmutableList<ProgramDefinition> activePrograms,
      ImmutableSet<ApplicationModel> applications,
      ImmutableList<ProgramDefinition> allPrograms) {
    // Use ImmutableMap.copyOf rather than the collector to guard against cases where the
    // provided active programs contains duplicate entries with the same adminName. In this
    // case, the ImmutableMap collector would throw since ImmutableMap builders don't allow
    // construction where the same key is provided twice. Using Collectors.toMap would just
    // use the last provided key.
    ImmutableMap<String, ProgramDefinition> activeProgramNames =
        ImmutableMap.copyOf(
            activePrograms.stream()
                .collect(Collectors.toMap(ProgramDefinition::adminName, pdef -> pdef)));

    // When new revisions of Programs are created, they have distinct IDs but retain the
    // same adminName. In order to find the most recent draft / active application,
    // we first group by the unique program name rather than the ID.
    Map<String, Map<LifecycleStage, Optional<ApplicationModel>>> mostRecentApplicationsByProgram =
        applications.stream()
            .collect(
                Collectors.groupingBy(
                    a -> {
                      return programRepository.getProgramDefinition(a.getProgram()).adminName();
                    },
                    Collectors.groupingBy(
                        ApplicationModel::getLifecycleStage,
                        // In practice, we don't expect an applicant to have multiple
                        // DRAFT or ACTIVE applications for a given program. Grabbing the latest
                        // application here guards against that case, should it occur.
                        Collectors.maxBy(
                            Comparator.<ApplicationModel, Instant>comparing(
                                    a -> {
                                      return a.getSubmitTime() != null
                                          ? a.getSubmitTime()
                                          : Instant.ofEpochMilli(0);
                                    })
                                .thenComparing(ApplicationModel::getCreateTime)))));

    ImmutableList.Builder<ApplicantProgramData> inProgressPrograms = ImmutableList.builder();
    ImmutableList.Builder<ApplicantProgramData> submittedPrograms = ImmutableList.builder();
    ImmutableList.Builder<ApplicantProgramData> unappliedPrograms = ImmutableList.builder();
    ApplicationPrograms.Builder relevantPrograms = ApplicationPrograms.builder();

    Set<String> programNamesWithApplications = Sets.newHashSet();
    mostRecentApplicationsByProgram.forEach(
        (programName, appByStage) -> {
          Optional<ApplicationModel> maybeDraftApp =
              appByStage.getOrDefault(LifecycleStage.DRAFT, Optional.empty());
          Optional<ApplicationModel> maybeSubmittedApp =
              appByStage.getOrDefault(LifecycleStage.ACTIVE, Optional.empty());
          Optional<Instant> latestSubmittedApplicationTime =
              maybeSubmittedApp.map(ApplicationModel::getSubmitTime);
          if (maybeDraftApp.isPresent()) {
            ApplicationModel draftApp = maybeDraftApp.get();
            // Get the program definition from the all programs list, since that has the
            // associated question data.
            ProgramDefinition programDefinition =
                findProgramWithId(allPrograms, draftApp.getProgram().id);
            ApplicantProgramData.Builder applicantProgramDataBuilder =
                ApplicantProgramData.builder()
                    .setProgram(programDefinition)
                    .setLatestSubmittedApplicationTime(latestSubmittedApplicationTime)
                    .setLatestApplicationLifecycleStage(Optional.of(LifecycleStage.DRAFT));
            applicantProgramDataBuilder.setIsProgramMaybeEligible(
                getApplicantMayBeEligibleStatus(draftApp.getApplicant(), programDefinition));
            if (programDefinition.isCommonIntakeForm()) {
              relevantPrograms.setCommonIntakeForm(applicantProgramDataBuilder.build());
            } else {
              inProgressPrograms.add(applicantProgramDataBuilder.build());
            }
            programNamesWithApplications.add(programName);
          } else if (maybeSubmittedApp.isPresent() && activeProgramNames.containsKey(programName)) {
            // When extracting the application status, the definitions associated with the program
            // version at the time of submission are used. However, when clicking "reapply", we use
            // the latest program version below.
            ProgramDefinition applicationProgramVersion =
                programRepository.getProgramDefinition(maybeSubmittedApp.get().getProgram());
            Optional<String> maybeLatestStatus = maybeSubmittedApp.get().getLatestStatus();
            Optional<StatusDefinitions.Status> maybeCurrentStatus =
                maybeLatestStatus.isPresent()
                    ? applicationProgramVersion.statusDefinitions().getStatuses().stream()
                        .filter(
                            programStatus ->
                                programStatus.statusText().equals(maybeLatestStatus.get()))
                        .findFirst()
                    : Optional.empty();

            // Get the program definition from the all programs list, since that has the
            // associated question data.
            ProgramDefinition programDefinition =
                findProgramWithId(allPrograms, activeProgramNames.get(programName).id());
            ApplicantProgramData.Builder applicantProgramDataBuilder =
                ApplicantProgramData.builder()
                    .setProgram(programDefinition)
                    .setLatestSubmittedApplicationTime(latestSubmittedApplicationTime)
                    .setLatestSubmittedApplicationStatus(maybeCurrentStatus)
                    .setLatestApplicationLifecycleStage(Optional.of(LifecycleStage.ACTIVE));

            applicantProgramDataBuilder.setIsProgramMaybeEligible(
                getApplicationEligibilityStatus(maybeSubmittedApp.get(), programDefinition));
            submittedPrograms.add(applicantProgramDataBuilder.build());
            programNamesWithApplications.add(programName);
          }
        });

    Set<String> unappliedActivePrograms =
        Sets.difference(activeProgramNames.keySet(), programNamesWithApplications);

    unappliedActivePrograms.forEach(
        programName -> {
          ApplicantProgramData.Builder applicantProgramDataBuilder =
              ApplicantProgramData.builder().setProgram(activeProgramNames.get(programName));
          ProgramDefinition program =
              findProgramWithId(allPrograms, activeProgramNames.get(programName).id());

          if (!mostRecentApplicationsByProgram.isEmpty()) {
            ApplicantModel applicant = applications.stream().findFirst().get().getApplicant();
            applicantProgramDataBuilder.setIsProgramMaybeEligible(
                getApplicantMayBeEligibleStatus(applicant, program));
          }

          if (program.isCommonIntakeForm()) {
            relevantPrograms.setCommonIntakeForm(applicantProgramDataBuilder.build());
          } else {
            unappliedPrograms.add(applicantProgramDataBuilder.build());
          }
        });

    return relevantPrograms
        .setInProgress(sortByProgramId(inProgressPrograms.build()))
        .setSubmitted(sortByProgramId(submittedPrograms.build()))
        .setUnapplied(sortByProgramId(unappliedPrograms.build()))
        .build();
  }

  private ProgramDefinition findProgramWithId(
      ImmutableList<ProgramDefinition> programList, long id) {
    return programList.stream().filter(p -> p.id() == id).findFirst().get();
  }

  private ImmutableList<ApplicantProgramData> sortByProgramId(
      ImmutableList<ApplicantProgramData> programs) {
    return programs.stream()
        .sorted(Comparator.comparing(p -> p.program().id()))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * The logging is to better understand a bug where some applicants were seeing duplicates of
   * programs for which they had draft applications. We can remove this logging once we determine
   * and resolve the root cause of the duplicate draft applications.
   */
  private void logDuplicateDrafts(ImmutableSet<ApplicationModel> applications) {
    Collection<Map<LifecycleStage, List<ApplicationModel>>> groupedByStatus =
        applications.stream()
            .collect(
                Collectors.groupingBy(
                    a -> {
                      return programRepository.getProgramDefinition(a.getProgram()).adminName();
                    },
                    Collectors.groupingBy(ApplicationModel::getLifecycleStage)))
            .values();
    for (Map<LifecycleStage, List<ApplicationModel>> programAppsMap : groupedByStatus) {
      List<ApplicationModel> draftApplications =
          programAppsMap.getOrDefault(LifecycleStage.DRAFT, Lists.newArrayList());
      if (draftApplications.size() > 1) {
        String joinedProgramIds =
            String.join(
                ", ",
                draftApplications.stream()
                    .map(
                        a ->
                            String.format(
                                "%d", programRepository.getProgramDefinition(a.getProgram()).id()))
                    .collect(ImmutableList.toImmutableList()));
        logger.debug(
            String.format(
                "DEBUG LOG ID: 98afa07855eb8e69338b5af13236a6b7. Program"
                    + " Admin Name: %1$s, Duplicate Program Definition"
                    + " ids: %2$s.",
                programRepository
                    .getProgramDefinition(draftApplications.get(0).getProgram())
                    .adminName(),
                joinedProgramIds));
      }
    }
  }

  /**
   * Relevant program data to be shown to the applicant, including the time at which the applicant
   * most recently submitted an application for some version of the program.
   */
  @AutoValue
  public abstract static class ApplicantProgramData {

    public long programId() {
      return program().id();
    }

    public abstract ProgramDefinition program();

    /**
     * Returns whether an applicant is potentially eligible for a program based only on the
     * questions they've answered, and empty if there are no eligibility conditions for the program.
     *
     * <p>If an applicant has not finished an application, only the questions they've answered are
     * used to determine if they might be eligible.
     */
    public abstract Optional<Boolean> isProgramMaybeEligible();

    public abstract Optional<Instant> latestSubmittedApplicationTime();

    public abstract Optional<StatusDefinitions.Status> latestSubmittedApplicationStatus();

    /**
     * LifecycleStage of the latest application to this program by this applicant, if an application
     * exists. ACTIVE if submitted, DRAFT if in progress.
     */
    public abstract Optional<LifecycleStage> latestApplicationLifecycleStage();

    public static Builder builder() {
      return new AutoValue_ApplicantService_ApplicantProgramData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setProgram(ProgramDefinition v);

      abstract Builder setIsProgramMaybeEligible(Optional<Boolean> v);

      abstract Builder setLatestSubmittedApplicationTime(Optional<Instant> v);

      abstract Builder setLatestSubmittedApplicationStatus(Optional<StatusDefinitions.Status> v);

      abstract Builder setLatestApplicationLifecycleStage(Optional<LifecycleStage> v);

      public abstract ApplicantProgramData build();
    }
  }

  /**
   * In-place update of {@link ApplicantData}. Adds program id and timestamp metadata with updates.
   *
   * @throws PathNotInBlockException if there are updates for questions that aren't in the block.
   * @throws UnsupportedScalarTypeException if there are updates for unsupported scalar types.
   * @return any failures to update the applicant data, containing the desired {@link Path} as well
   *     as the raw string value that failed update.
   */
  private ImmutableMap<Path, String> stageUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates,
      Optional<ServiceAreaUpdate> serviceAreaUpdate)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    if (block.isEnumerator()) {
      return stageEnumeratorUpdates(applicantData, block, updateMetadata, updates);
    } else {
      return stageNormalUpdates(applicantData, block, updateMetadata, updates, serviceAreaUpdate);
    }
  }

  /**
   * Stage updates for an enumerator.
   *
   * @return any failures to update the applicant data, containing the desired {@link Path} as well
   *     as the raw string value that failed update.
   * @throws PathNotInBlockException for updates that aren't {@link Scalar#ENTITY_NAME}, or {@link
   *     Scalar#DELETE_ENTITY}.
   */
  private ImmutableMap<Path, String> stageEnumeratorUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates)
      throws PathNotInBlockException {
    Path enumeratorPath = block.getEnumeratorQuestion().getContextualizedPath();

    ImmutableSet<Update> addsAndChanges = validateEnumeratorAddsAndChanges(block, updates);
    ImmutableSet<Update> deletes =
        updates.stream()
            .filter(
                update ->
                    update
                        .path()
                        .withoutArrayReference()
                        .equals(Path.empty().join(Scalar.DELETE_ENTITY)))
            .collect(ImmutableSet.toImmutableSet());

    // If there are more updates than there are adds/changes and deletes, throw
    Set<Update> unknownUpdates = Sets.difference(updates, Sets.union(addsAndChanges, deletes));
    if (!unknownUpdates.isEmpty()) {
      throw new PathNotInBlockException(block.getId(), unknownUpdates.iterator().next().path());
    }

    // Before adding anything, if only metadata is being stored then delete it. We cannot put an
    // array of entities at the question path if a JSON object with metadata is already at that
    // path.
    if (applicantData.readRepeatedEntities(enumeratorPath).isEmpty()) {
      applicantData.maybeDelete(enumeratorPath.withoutArrayReference());
    }

    // Add and change entity names BEFORE deleting, because if deletes happened first, then changed
    // entity names may not match the intended entities.
    for (Update update : addsAndChanges) {
      applicantData.putString(update.path().join(Scalar.ENTITY_NAME), update.value());
      writeMetadataForPath(update.path(), applicantData, updateMetadata);
    }

    ImmutableList<Integer> deleteIndices =
        deletes.stream()
            .map(Update::value)
            .map(Integer::valueOf)
            .collect(ImmutableList.toImmutableList());
    applicantData.deleteRepeatedEntities(enumeratorPath, deleteIndices);

    // If there are no repeated entities at this point, we still need to save metadata for this
    // question.
    if (applicantData.maybeClearRepeatedEntities(enumeratorPath)) {
      writeMetadataForPath(enumeratorPath.withoutArrayReference(), applicantData, updateMetadata);
    }
    return ImmutableMap.of();
  }

  /**
   * Validate that the updates to add or change enumerated entity names have the correct paths with
   * the right indices.
   */
  private ImmutableSet<Update> validateEnumeratorAddsAndChanges(
      Block block, ImmutableSet<Update> updates) {
    ImmutableSet<Update> entityUpdates =
        updates.stream()
            .filter(
                update ->
                    update
                        .path()
                        .withoutArrayReference()
                        .equals(
                            block
                                .getEnumeratorQuestion()
                                .getContextualizedPath()
                                .withoutArrayReference()))
            .collect(ImmutableSet.toImmutableSet());

    // Early return if it is empty.
    if (entityUpdates.isEmpty()) {
      return entityUpdates;
    }

    // Check that the entity updates have unique and consecutive indices. The indices should be
    // 0,...N-1 where N is entityUpdates.size() because all entity names are submitted in the form,
    // whether or not they actually changed.
    ImmutableSet<Integer> indices =
        entityUpdates.stream()
            .map(update -> update.path().arrayIndex())
            .collect(ImmutableSet.toImmutableSet());
    assert indices.size() == entityUpdates.size();
    assert indices.stream().min(Comparator.naturalOrder()).get() == 0;
    assert indices.stream().max(Comparator.naturalOrder()).get() == entityUpdates.size() - 1;

    return entityUpdates;
  }

  /**
   * In-place update of {@link ApplicantData}. Adds program id and timestamp metadata with updates.
   *
   * @return any failures to update the applicant data, containing the desired {@link Path} as well
   *     as the raw string value that failed update.
   * @throws PathNotInBlockException if there are updates for questions that aren't in the block.
   * @throws UnsupportedScalarTypeException if there are updates for unsupported scalar types.
   */
  private ImmutableMap<Path, String> stageNormalUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates,
      Optional<ServiceAreaUpdate> serviceAreaUpdate)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    ArrayList<Path> visitedPaths = new ArrayList<>();
    ImmutableMap.Builder<Path, String> failedUpdatesBuilder = ImmutableMap.builder();
    for (Update update : updates) {
      Path currentPath = update.path();

      // If we're updating an array we need to clear it the first time it is visited
      if (currentPath.isArrayElement()
          && !visitedPaths.contains(currentPath.withoutArrayReference())) {
        visitedPaths.add(currentPath.withoutArrayReference());
        applicantData.maybeClearArray(currentPath);
      }

      ScalarType type =
          block
              .getScalarType(currentPath)
              .orElseThrow(() -> new PathNotInBlockException(block.getId(), currentPath));
      // An empty update means the applicant doesn't want to store anything. We already cleared the
      // multi-select array above in preparation for updates, so do not remove the path.
      if (!update.path().isArrayElement() && update.value().isBlank()) {
        applicantData.maybeDelete(update.path());
      } else {
        switch (type) {
          case PHONE_NUMBER:
            try {
              applicantData.putPhoneNumber(currentPath, update.value());
            } catch (IllegalArgumentException e) {
              failedUpdatesBuilder.put(currentPath, update.value());
            }
            break;
          case CURRENCY_CENTS:
            try {
              applicantData.putCurrencyDollars(currentPath, update.value());
            } catch (IllegalArgumentException e) {
              failedUpdatesBuilder.put(currentPath, update.value());
            }
            break;
          case DATE:
            try {
              applicantData.putDate(currentPath, update.value());
            } catch (DateTimeParseException e) {
              failedUpdatesBuilder.put(currentPath, update.value());
            }
            break;
          case LIST_OF_STRINGS:
          case STRING:
            applicantData.putString(currentPath, update.value());
            break;
          case LONG:
            try {
              applicantData.putLong(currentPath, update.value());
            } catch (NumberFormatException e) {
              failedUpdatesBuilder.put(currentPath, update.value());
            }
            break;
          case DOUBLE:
            try {
              applicantData.putDouble(currentPath, update.value());
            } catch (NumberFormatException e) {
              failedUpdatesBuilder.put(currentPath, update.value());
            }
            break;
          case SERVICE_AREA:
            // service areas get updated below
            break;
          default:
            throw new UnsupportedScalarTypeException(type);
        }
      }
    }

    if (serviceAreaUpdate.isPresent() && serviceAreaUpdate.get().value().size() > 0) {
      applicantData.putString(
          serviceAreaUpdate.get().path(),
          ServiceAreaInclusionGroup.serialize(serviceAreaUpdate.get().value()));
    }

    // Write metadata for all questions in the block, regardless of whether they were blank or not.
    block.getQuestions().stream()
        .map(ApplicantQuestion::getContextualizedPath)
        .forEach(path -> writeMetadataForPath(path, applicantData, updateMetadata));
    return failedUpdatesBuilder.build();
  }

  private void writeMetadataForPath(Path path, ApplicantData data, UpdateMetadata updateMetadata) {
    data.putLong(path.join(Scalar.PROGRAM_UPDATED_IN), updateMetadata.programId());
    data.putLong(path.join(Scalar.UPDATED_AT), updateMetadata.updatedAt());
  }

  @AutoValue
  abstract static class UpdateMetadata {

    static UpdateMetadata create(long programId, long updatedAt) {
      return new AutoValue_ApplicantService_UpdateMetadata(programId, updatedAt);
    }

    abstract long programId();

    abstract long updatedAt();
  }

  /**
   * A categorized list of relevant {@link ApplicantProgramData}s to be displayed to the applicant.
   */
  @AutoValue
  public abstract static class ApplicationPrograms {
    /**
     * Common Intake Form, if it exists, is populated only here and not in inProgress, submitted, or
     * unapplied regardless of its application status.
     */
    public abstract Optional<ApplicantProgramData> commonIntakeForm();

    public abstract ImmutableList<ApplicantProgramData> inProgress();

    public abstract ImmutableList<ApplicantProgramData> submitted();

    public abstract ImmutableList<ApplicantProgramData> unapplied();

    /**
     * Programs the applicant has not applied to and has no questions the applicant has answered
     * with eligibility criteria that the applicant does not meet.
     */
    public ImmutableList<ApplicantProgramData> unappliedAndPotentiallyEligible() {
      return unapplied().stream()
          .filter(
              (ApplicantService.ApplicantProgramData applicantProgramData) ->
                  applicantProgramData.isProgramMaybeEligible().orElse(true))
          .collect(ImmutableList.toImmutableList());
    }

    static Builder builder() {
      return new AutoValue_ApplicantService_ApplicationPrograms.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setCommonIntakeForm(ApplicantProgramData value);

      abstract Builder setInProgress(ImmutableList<ApplicantProgramData> value);

      abstract Builder setSubmitted(ImmutableList<ApplicantProgramData> value);

      abstract Builder setUnapplied(ImmutableList<ApplicantProgramData> value);

      abstract ApplicationPrograms build();
    }

    /** Returns all relevant programs for the applicant. */
    public ImmutableList<ApplicantProgramData> allPrograms() {
      ImmutableList.Builder<ApplicantProgramData> allPrograms =
          new ImmutableList.Builder<ApplicantProgramData>();

      if (commonIntakeForm().isPresent()) {
        allPrograms.add(commonIntakeForm().get());
      }
      allPrograms.addAll(inProgress());
      allPrograms.addAll(submitted());
      allPrograms.addAll(unapplied());
      return allPrograms.build();
    }
  }

  /**
   * Get corrected address from Esri and formats it as a map compatible with form data. It matches
   * the user selected address and looks that up in the list of suggestions retrieved earlier from
   * the Esri service. If ServiceArea validation is not enabled on this block the user is able to
   * elect to keep the address as they entered it.
   *
   * <p>Returns a map containing the corrected address along with the corrected state in a format
   * ready to be saved as form data.
   */
  public CompletionStage<ImmutableMap<String, String>> getCorrectedAddress(
      long applicantId,
      long programId,
      String blockId,
      Optional<String> selectedAddress,
      ImmutableList<AddressSuggestion> addressSuggestions) {
    return getReadOnlyApplicantProgramService(applicantId, programId)
        .thenComposeAsync(
            roApplicantProgramService -> {
              Optional<Block> blockMaybe = roApplicantProgramService.getActiveBlock(blockId);

              if (blockMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ProgramBlockNotFoundException(programId, blockId));
              }

              ApplicantQuestion applicantQuestion =
                  getFirstAddressCorrectionEnabledApplicantQuestion(blockMaybe.get());
              AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

              Optional<AddressSuggestion> suggestionMaybe =
                  addressSuggestions.stream()
                      .filter(
                          addressSuggestion ->
                              addressSuggestion
                                  .getSingleLineAddress()
                                  .equals(selectedAddress.orElse("")))
                      .findFirst();

              ImmutableMap<String, String> questionPathToValueMap =
                  buildCorrectedAddressAsFormData(
                      applicantId,
                      programId,
                      blockId,
                      addressQuestion,
                      suggestionMaybe,
                      selectedAddress);

              return CompletableFuture.completedFuture(questionPathToValueMap);
            });
  }

  /** Maps address suggestion and corrected state into a form data compatible map */
  private ImmutableMap<String, String> buildCorrectedAddressAsFormData(
      long applicantId,
      long programId,
      String blockId,
      AddressQuestion addressQuestion,
      Optional<AddressSuggestion> suggestionMaybe,
      Optional<String> selectedAddress) {

    ImmutableMap.Builder<String, String> questionPathToValueMap = ImmutableMap.builder();

    if (suggestionMaybe.isPresent()) {
      AddressSuggestion suggestion = suggestionMaybe.get();
      Address address = suggestion.getAddress();
      AddressLocation location = suggestion.getLocation();

      questionPathToValueMap.put(addressQuestion.getStreetPath().toString(), address.getStreet());
      questionPathToValueMap.put(addressQuestion.getLine2Path().toString(), address.getLine2());
      questionPathToValueMap.put(addressQuestion.getCityPath().toString(), address.getCity());
      questionPathToValueMap.put(addressQuestion.getStatePath().toString(), address.getState());
      questionPathToValueMap.put(addressQuestion.getZipPath().toString(), address.getZip());
      questionPathToValueMap.put(
          addressQuestion.getLatitudePath().toString(), location.getLatitude().toString());
      questionPathToValueMap.put(
          addressQuestion.getLongitudePath().toString(), location.getLongitude().toString());
      questionPathToValueMap.put(
          addressQuestion.getWellKnownIdPath().toString(), location.getWellKnownId().toString());
      questionPathToValueMap.put(
          addressQuestion.getCorrectedPath().toString(),
          CorrectedAddressState.CORRECTED.getSerializationFormat());
    } else if (selectedAddress.isPresent()
        && selectedAddress.get().equals(AddressCorrectionBlockView.USER_KEEPING_ADDRESS_VALUE)) {
      questionPathToValueMap.put(
          addressQuestion.getCorrectedPath().toString(),
          CorrectedAddressState.AS_ENTERED_BY_USER.getSerializationFormat());
    } else {
      questionPathToValueMap.put(
          addressQuestion.getCorrectedPath().toString(),
          CorrectedAddressState.FAILED.getSerializationFormat());
      logger.error(
          "Address correction failed for applicantId: {} programId: {} blockId: {}",
          applicantId,
          programId,
          blockId);
    }

    return questionPathToValueMap.build();
  }

  /**
   * Finds the first {@link ApplicantQuestion} that is an address question type and has address
   * correction enabled. When address correction is enabled we will make calls to the Esri service
   * to check the user supplied address. The passed in {@link Block} contains the metadata used to
   * determine if address correction is enabled for a question.
   */
  public ApplicantQuestion getFirstAddressCorrectionEnabledApplicantQuestion(Block block) {
    Optional<ApplicantQuestion> applicantQuestionMaybe =
        block.getAddressQuestionWithCorrectionEnabled();

    if (applicantQuestionMaybe.isEmpty()) {
      throw new RuntimeException(
          String.format(
              "Expected to find an address with address correction enabled in block %s, but did"
                  + " not.",
              block.getId()));
    }

    return applicantQuestionMaybe.get();
  }

  /** Gets address suggestions */
  public CompletionStage<AddressSuggestionGroup> getAddressSuggestionGroup(Block block) {
    ApplicantQuestion applicantQuestion = getFirstAddressCorrectionEnabledApplicantQuestion(block);
    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();
    return esriClient.getAddressSuggestions(addressQuestion.getAddress());
  }

  /**
   * Checks for an {@link AddressQuestion} that has address correction enabled. If found and the
   * data in the database differs from the submitted form data. Set the corrected, latitude, and
   * longitude, and wellKnownId values to empty. This is done to allow triggering the address
   * correction process again, but only when there have been changes.
   *
   * <p>Otherwise if no {@link AddressQuestion} or no changes to the address we return the untouched
   * formdata
   */
  public CompletionStage<ImmutableMap<String, String>> resetAddressCorrectionWhenAddressChanged(
      long applicantId, long programId, String blockId, ImmutableMap<String, String> formData) {
    return getReadOnlyApplicantProgramService(applicantId, programId)
        .thenComposeAsync(
            roApplicantProgramService -> {
              Optional<Block> blockMaybe = roApplicantProgramService.getActiveBlock(blockId);

              if (blockMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ProgramBlockNotFoundException(programId, blockId));
              }

              Optional<ApplicantQuestion> addressQuestionMaybe =
                  blockMaybe.get().getAddressQuestionWithCorrectionEnabled();

              if (addressQuestionMaybe.isEmpty()) {
                return CompletableFuture.completedFuture(formData);
              }

              AddressQuestion addressQuestion = addressQuestionMaybe.get().createAddressQuestion();

              if (addressQuestion.hasChanges(formData)) {
                return CompletableFuture.completedFuture(
                    new ImmutableMap.Builder<String, String>()
                        .putAll(formData)
                        .put(addressQuestion.getCorrectedPath().toString(), "")
                        .put(addressQuestion.getLatitudePath().toString(), "")
                        .put(addressQuestion.getLongitudePath().toString(), "")
                        .put(addressQuestion.getWellKnownIdPath().toString(), "")
                        .build());
              }

              return CompletableFuture.completedFuture(formData);
            });
  }

  /**
   * Checks the block for any {@link PhoneQuestion}. If any are found grab the phone number from the
   * formData and call the {@link PhoneValidationUtils#validatePhoneNumberWithCountryCode} to
   * calculate the country code.
   */
  public CompletionStage<ImmutableMap<String, String>> setPhoneCountryCode(
      long applicantId, long programId, String blockId, ImmutableMap<String, String> formData) {
    return getReadOnlyApplicantProgramService(applicantId, programId)
        .thenComposeAsync(
            roApplicantProgramService -> {
              Optional<Block> blockMaybe = roApplicantProgramService.getActiveBlock(blockId);

              if (blockMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ProgramBlockNotFoundException(programId, blockId));
              }

              // Get a writeable map so the existing paths can be replaced
              Map<String, String> newFormData = new java.util.HashMap<>(formData);

              for (ApplicantQuestion applicantQuestion : blockMaybe.get().getQuestions()) {
                if (applicantQuestion.getType() != QuestionType.PHONE) {
                  continue;
                }

                PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

                Optional<String> phoneNumber =
                    Optional.of(newFormData.get(phoneQuestion.getPhoneNumberPath().toString()));

                PhoneValidationResult result =
                    PhoneValidationUtils.determineCountryCode(phoneNumber);

                if (result.isValid()) {
                  newFormData.put(
                      phoneQuestion.getCountryCodePath().toString(),
                      result.getCountryCode().orElse(""));
                }
              }

              return CompletableFuture.completedFuture(ImmutableMap.copyOf(newFormData));
            });
  }
}
