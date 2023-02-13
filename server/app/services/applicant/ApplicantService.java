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
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.LifecycleStage;
import models.Program;
import models.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicationRepository;
import repository.StoredFileRepository;
import repository.TimeFilter;
import repository.UserRepository;
import repository.VersionRepository;
import services.Path;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ApplicationNotEligibleException;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.cloud.aws.SimpleEmail;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.ScalarType;

/**
 * The service responsible for accessing the Applicant resource. Applicants can view program
 * applications defined by the {@link services.program.ProgramService} as a series of {@link
 * Block}s, one per-page. When an applicant submits the form for a Block the ApplicantService is
 * responsible for validating and persisting their answers and then providing the next Block for
 * them to view, if any.
 */
public final class ApplicantService {
  private static final Logger logger = LoggerFactory.getLogger(ApplicantService.class);

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final StoredFileRepository storedFileRepository;
  private final VersionRepository versionRepository;
  private final ProgramService programService;
  private final SimpleEmail amazonSESClient;
  private final Clock clock;
  private final String baseUrl;
  private final boolean isStaging;
  private final HttpExecutionContext httpExecutionContext;
  private final String stagingProgramAdminNotificationMailingList;
  private final String stagingTiNotificationMailingList;
  private final String stagingApplicantNotificationMailingList;

  @Inject
  public ApplicantService(
      ApplicationRepository applicationRepository,
      UserRepository userRepository,
      VersionRepository versionRepository,
      StoredFileRepository storedFileRepository,
      ProgramService programService,
      SimpleEmail amazonSESClient,
      Clock clock,
      Config configuration,
      HttpExecutionContext httpExecutionContext) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.userRepository = checkNotNull(userRepository);
    this.versionRepository = checkNotNull(versionRepository);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.programService = checkNotNull(programService);
    this.amazonSESClient = checkNotNull(amazonSESClient);
    this.clock = checkNotNull(clock);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);

    String stagingHostname = checkNotNull(configuration).getString("staging_hostname");
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.isStaging = URI.create(baseUrl).getHost().equals(stagingHostname);
    this.stagingProgramAdminNotificationMailingList =
        checkNotNull(configuration).getString("staging_program_admin_notification_mailing_list");
    this.stagingTiNotificationMailingList =
        checkNotNull(configuration).getString("staging_ti_notification_mailing_list");
    this.stagingApplicantNotificationMailingList =
        checkNotNull(configuration).getString("staging_applicant_notification_mailing_list");
  }

  /** Create a new {@link Applicant}. */
  public CompletionStage<Applicant> createApplicant() {

    Applicant applicant = new Applicant();
    return userRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
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
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        userRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getActiveProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              Applicant applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();

              return new ReadOnlyApplicantProgramServiceImpl(
                  applicant.getApplicantData(), programDefinition, baseUrl);
            },
            httpExecutionContext.current());
  }

  /** Get a {@link ReadOnlyApplicantProgramService} from an application. */
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      Application application) {
    try {
      return CompletableFuture.completedFuture(
          new ReadOnlyApplicantProgramServiceImpl(
              application.getApplicantData(),
              programService.getProgramDefinition(application.getProgram().id),
              baseUrl));
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException("Cannot find a program that has applications for it.", e);
    }
  }

  /** Get a {@link ReadOnlyApplicantProgramService} from an application and program definition. */
  public ReadOnlyApplicantProgramService getReadOnlyApplicantProgramService(
      Application application, ProgramDefinition programDefinition) {
    return new ReadOnlyApplicantProgramServiceImpl(
        application.getApplicantData(), programDefinition, baseUrl);
  }

  /**
   * Attempt to perform a set of updates to the applicant's {@link ApplicantData}. If updates are
   * valid, they are saved to storage. If not, a set of errors are returned along with the modified
   * {@link ApplicantData}, but none of the updates are persisted to storage.
   *
   * <p>Updates are atomic i.e. if any of them fail validation, none of them will be written.
   *
   * @return a {@link ReadOnlyApplicantProgramService} that reflects the updates regardless of
   *     whether they are presisted or not, which may have invalid data with errors associated with
   *     it. If the service cannot perform the update due to exceptions, they are wrapped in
   *     `CompletionException`s.
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
      long applicantId, long programId, String blockId, ImmutableMap<String, String> updateMap) {
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

    return stageAndUpdateIfValid(applicantId, programId, blockId, updates);
  }

  private CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      long applicantId, long programId, String blockId, ImmutableSet<Update> updates) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        userRepository.lookupApplicant(applicantId).toCompletableFuture();

    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getActiveProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenComposeAsync(
            (v) -> {
              Optional<Applicant> applicantMaybe = applicantCompletableFuture.join();
              if (applicantMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(new ApplicantNotFoundException(applicantId));
              }
              Applicant applicant = applicantMaybe.get();

              // Create a ReadOnlyApplicantProgramService and get the current block.
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();
              ReadOnlyApplicantProgramService readOnlyApplicantProgramServiceBeforeUpdate =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition, baseUrl);
              Optional<Block> maybeBlockBeforeUpdate =
                  readOnlyApplicantProgramServiceBeforeUpdate.getBlock(blockId);
              if (maybeBlockBeforeUpdate.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ProgramBlockNotFoundException(programId, blockId));
              }
              Block blockBeforeUpdate = maybeBlockBeforeUpdate.get();

              UpdateMetadata updateMetadata = UpdateMetadata.create(programId, clock.millis());
              ImmutableMap<Path, String> failedUpdates;
              try {
                failedUpdates =
                    stageUpdates(
                        applicant.getApplicantData(), blockBeforeUpdate, updateMetadata, updates);
              } catch (UnsupportedScalarTypeException | PathNotInBlockException e) {
                return CompletableFuture.failedFuture(e);
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition, baseUrl, failedUpdates);

              Optional<Block> blockMaybe = roApplicantProgramService.getBlock(blockId);
              if (blockMaybe.isPresent() && !blockMaybe.get().hasErrors()) {
                return userRepository
                    .updateApplicant(applicant)
                    .thenApplyAsync(
                        (finishedSaving) -> roApplicantProgramService,
                        httpExecutionContext.current());
              }

              return CompletableFuture.completedFuture(roApplicantProgramService);
            },
            httpExecutionContext.current())
        .thenCompose(
            (v) ->
                applicationRepository
                    .createOrUpdateDraft(applicantId, programId)
                    .thenApplyAsync(appDraft -> v));
  }

  /**
   * Create a new active {@link Application} for the applicant applying to the program.
   *
   * <p>An application is a snapshot of all the answers the applicant has filled in so far, along
   * with association with the applicant and a program that the applicant is applying to.
   *
   * @param submitterProfile the user that submitted the application, iff it is a TI the application
   *     is associated with this profile too.
   * @return the saved {@link Application}. If the submission failed, a {@link
   *     ApplicationSubmissionException} is thrown and wrapped in a `CompletionException`.
   */
  public CompletionStage<Application> submitApplication(
      long applicantId,
      long programId,
      CiviFormProfile submitterProfile,
      boolean eligibilityFeatureEnabled) {
    if (submitterProfile.isTrustedIntermediary()) {
      return getReadOnlyApplicantProgramService(applicantId, programId)
          .thenCompose(ro -> validateApplicationForSubmission(ro, eligibilityFeatureEnabled))
          .thenCompose(v -> submitterProfile.getAccount())
          .thenComposeAsync(
              account ->
                  submitApplication(
                      applicantId,
                      programId,
                      /* tiSubmitterEmail= */ Optional.of(account.getEmailAddress())),
              httpExecutionContext.current());
    }

    return getReadOnlyApplicantProgramService(applicantId, programId)
        .thenCompose(ro -> validateApplicationForSubmission(ro, eligibilityFeatureEnabled))
        .thenCompose(
            v ->
                submitApplication(
                    applicantId, programId, /* tiSubmitterEmail= */ Optional.empty()));
  }

  @VisibleForTesting
  CompletionStage<Application> submitApplication(
      long applicantId, long programId, Optional<String> tiSubmitterEmail) {
    return applicationRepository
        .submitApplication(applicantId, programId, tiSubmitterEmail)
        .thenComposeAsync(
            (Optional<Application> applicationMaybe) -> {
              if (applicationMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ApplicationSubmissionException(applicantId, programId));
              }

              Application application = applicationMaybe.get();
              String programName = application.getProgram().getProgramDefinition().adminName();
              notifyProgramAdmins(applicantId, programId, application.id, programName);

              if (tiSubmitterEmail.isPresent()) {
                notifyTiSubmitter(tiSubmitterEmail.get(), applicantId, application.id, programName);
              }

              maybeNotifyApplicant(applicantId, application.id, programName);

              return updateStoredFileAclsForSubmit(applicantId, programId)
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
   * @return a {@link ApplicationOutOfDateException} wrapped in a failed future with a user visible
   *     message for the issue.
   */
  private CompletableFuture<Void> validateApplicationForSubmission(
      ReadOnlyApplicantProgramService roApplicantProgramService,
      boolean eligibilityFeatureEnabled) {
    // Check that all blocks have been answered.
    if (!roApplicantProgramService.getFirstIncompleteBlockExcludingStatic().isEmpty()) {
      throw new ApplicationOutOfDateException();
    }
    if (eligibilityFeatureEnabled && !roApplicantProgramService.isApplicationEligible()) {
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
        programService.getActiveProgramDefinitionAsync(programId).toCompletableFuture();

    CompletableFuture<List<StoredFile>> storedFilesFuture =
        getReadOnlyApplicantProgramService(applicantId, programId)
            .thenApplyAsync(
                ReadOnlyApplicantProgramService::getStoredFileKeys, httpExecutionContext.current())
            .thenComposeAsync(storedFileRepository::lookupFiles, httpExecutionContext.current())
            .toCompletableFuture();

    return CompletableFuture.allOf(programDefinitionCompletableFuture, storedFilesFuture)
        .thenComposeAsync(
            (ignoreVoid) -> {
              List<StoredFile> storedFiles = storedFilesFuture.join();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();
              CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

              for (StoredFile file : storedFiles) {
                file.getAcls().addProgramToReaders(programDefinition);
                future =
                    CompletableFuture.allOf(
                        future, storedFileRepository.update(file).toCompletableFuture());
              }

              return future;
            },
            httpExecutionContext.current());
  }

  private void notifyProgramAdmins(
      long applicantId, long programId, long applicationId, String programName) {
    String viewLink =
        baseUrl
            + controllers.admin.routes.AdminApplicationController.show(programId, applicationId)
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

  private void notifyTiSubmitter(
      String tiEmail, long applicantId, long applicationId, String programName) {
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dateQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    String subject =
        String.format(
            "You submitted an application for program %s on behalf of applicant %d",
            programName, applicantId);
    String message =
        String.format(
            "The application to program %s as applicant %d has been received, and the application"
                + " ID is %d.\n"
                + "Manage your clients at %s.",
            programName, applicantId, applicationId, tiDashLink);
    if (isStaging) {
      amazonSESClient.send(stagingTiNotificationMailingList, subject, message);
    } else {
      amazonSESClient.send(tiEmail, subject, message);
    }
  }

  private void maybeNotifyApplicant(long applicantId, long applicationId, String programName) {
    Optional<String> email = getEmail(applicantId).toCompletableFuture().join();
    if (email.isEmpty()) {
      return;
    }
    String civiformLink = baseUrl;
    String subject = String.format("Your application to program %s is received", programName);
    String message =
        String.format(
            "Your application to program %s has been received. Your applicant ID is %d and the"
                + " application ID is %d.\n"
                + "Log in to CiviForm at %s.",
            programName, applicantId, applicationId, civiformLink);
    if (isStaging) {
      amazonSESClient.send(stagingApplicantNotificationMailingList, subject, message);
    } else {
      amazonSESClient.send(email.get(), subject, message);
    }
  }

  /** Return the name of the given applicant id. If not available, returns the email. */
  public CompletionStage<Optional<String>> getNameOrEmail(long applicantId) {
    return userRepository
        .lookupApplicant(applicantId)
        .thenApplyAsync(
            applicant -> {
              if (applicant.isEmpty()) {
                return Optional.empty();
              }
              Optional<String> name = applicant.get().getApplicantData().getApplicantName();
              if (name.isPresent() && !Strings.isNullOrEmpty(name.get())) {
                return name;
              }
              String emailAddress = applicant.get().getAccount().getEmailAddress();
              if (!Strings.isNullOrEmpty(emailAddress)) {
                return Optional.of(emailAddress);
              }
              return Optional.empty();
            },
            httpExecutionContext.current());
  }

  /** Return the name of the given applicant id. */
  public CompletionStage<Optional<String>> getName(long applicantId) {
    return userRepository
        .lookupApplicant(applicantId)
        .thenApplyAsync(
            applicant -> {
              if (applicant.isEmpty()) {
                return Optional.empty();
              }
              return applicant.get().getApplicantData().getApplicantName();
            },
            httpExecutionContext.current());
  }

  /** Return the email of the given applicant id if they have one. */
  public CompletionStage<Optional<String>> getEmail(long applicantId) {
    return userRepository
        .lookupApplicant(applicantId)
        .thenApplyAsync(
            applicant -> {
              if (applicant.isEmpty()) {
                return Optional.empty();
              }
              String emailAddress = applicant.get().getAccount().getEmailAddress();
              if (Strings.isNullOrEmpty(emailAddress)) {
                return Optional.empty();
              }
              return Optional.of(emailAddress);
            },
            httpExecutionContext.current());
  }

  /**
   * Return a filtered set of applications, including applications from previous versions, with
   * program, applicant, and account associations eager loaded. Results are ordered by application
   * ID in ascending order.
   */
  public ImmutableList<Application> getApplications(TimeFilter submitTimeFilter) {
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
  public CompletionStage<ApplicationPrograms> relevantProgramsForApplicant(long applicantId) {
    // Note: The Program model associated with the application is eagerly loaded.
    CompletableFuture<ImmutableSet<Application>> applicationsFuture =
        applicationRepository
            .getApplicationsForApplicant(
                applicantId, ImmutableSet.of(LifecycleStage.DRAFT, LifecycleStage.ACTIVE))
            .toCompletableFuture();
    ImmutableList<ProgramDefinition> activePrograms =
        versionRepository.getActiveVersion().getPrograms().stream()
            .map(Program::getProgramDefinition)
            .filter(pdef -> pdef.displayMode().equals(DisplayMode.PUBLIC))
            .collect(ImmutableList.toImmutableList());

    return applicationsFuture.thenApplyAsync(
        applications -> {
          logDuplicateDrafts(applications);
          return relevantProgramsForApplicant(activePrograms, applications);
        },
        httpExecutionContext.current());
  }

  private ApplicationPrograms relevantProgramsForApplicant(
      ImmutableList<ProgramDefinition> activePrograms, ImmutableSet<Application> applications) {
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
    Map<String, Map<LifecycleStage, Optional<Application>>> mostRecentApplicationsByProgram =
        applications.stream()
            .collect(
                Collectors.groupingBy(
                    a -> {
                      return a.getProgram().getProgramDefinition().adminName();
                    },
                    Collectors.groupingBy(
                        Application::getLifecycleStage,
                        // In practice, we don't expect an applicant to have multiple
                        // DRAFT or ACTIVE applications for a given program. Grabbing the latest
                        // application here guards against that case, should it occur.
                        Collectors.maxBy(
                            Comparator.<Application, Instant>comparing(
                                    a -> {
                                      return a.getSubmitTime() != null
                                          ? a.getSubmitTime()
                                          : Instant.ofEpochMilli(0);
                                    })
                                .thenComparing(Application::getCreateTime)))));

    ImmutableList.Builder<ApplicantProgramData> inProgressPrograms = ImmutableList.builder();
    ImmutableList.Builder<ApplicantProgramData> submittedPrograms = ImmutableList.builder();
    ImmutableList.Builder<ApplicantProgramData> unappliedPrograms = ImmutableList.builder();

    Set<String> programNamesWithApplications = Sets.newHashSet();
    mostRecentApplicationsByProgram.forEach(
        (programName, appByStage) -> {
          Optional<Application> maybeDraftApp =
              appByStage.getOrDefault(LifecycleStage.DRAFT, Optional.empty());
          Optional<Application> maybeSubmittedApp =
              appByStage.getOrDefault(LifecycleStage.ACTIVE, Optional.empty());
          Optional<Instant> latestSubmittedApplicationTime =
              maybeSubmittedApp.map(Application::getSubmitTime);
          if (maybeDraftApp.isPresent()) {
            inProgressPrograms.add(
                ApplicantProgramData.builder()
                    .setProgram(maybeDraftApp.get().getProgram().getProgramDefinition())
                    .setLatestSubmittedApplicationTime(latestSubmittedApplicationTime)
                    .build());
            programNamesWithApplications.add(programName);
          } else if (maybeSubmittedApp.isPresent() && activeProgramNames.containsKey(programName)) {
            // When extracting the application status, the definitions associated with the program
            // version at the time of submission are used. However, when clicking "reapply", we use
            // the latest program version below.
            ProgramDefinition applicationProgramVersion =
                maybeSubmittedApp.get().getProgram().getProgramDefinition();
            Optional<String> maybeLatestStatus = maybeSubmittedApp.get().getLatestStatus();
            Optional<StatusDefinitions.Status> maybeCurrentStatus =
                maybeLatestStatus.isPresent()
                    ? applicationProgramVersion.statusDefinitions().getStatuses().stream()
                        .filter(
                            programStatus ->
                                programStatus.statusText().equals(maybeLatestStatus.get()))
                        .findFirst()
                    : Optional.empty();
            submittedPrograms.add(
                ApplicantProgramData.builder()
                    .setProgram(activeProgramNames.get(programName))
                    .setLatestSubmittedApplicationTime(latestSubmittedApplicationTime)
                    .setLatestSubmittedApplicationStatus(maybeCurrentStatus)
                    .build());
            programNamesWithApplications.add(programName);
          }
        });

    Set<String> unappliedActivePrograms =
        Sets.difference(activeProgramNames.keySet(), programNamesWithApplications);
    unappliedActivePrograms.forEach(
        programName -> {
          unappliedPrograms.add(
              ApplicantProgramData.builder()
                  .setProgram(activeProgramNames.get(programName))
                  .build());
        });

    // Ensure each list is ordered by database ID for consistent ordering.
    return ApplicationPrograms.builder()
        .setInProgress(sortByProgramId(inProgressPrograms.build()))
        .setSubmitted(sortByProgramId(submittedPrograms.build()))
        .setUnapplied(sortByProgramId(unappliedPrograms.build()))
        .build();
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
  private void logDuplicateDrafts(ImmutableSet<Application> applications) {
    Collection<Map<LifecycleStage, List<Application>>> groupedByStatus =
        applications.stream()
            .collect(
                Collectors.groupingBy(
                    a -> {
                      return a.getProgram().getProgramDefinition().adminName();
                    },
                    Collectors.groupingBy(Application::getLifecycleStage)))
            .values();
    for (Map<LifecycleStage, List<Application>> programAppsMap : groupedByStatus) {
      List<Application> draftApplications =
          programAppsMap.getOrDefault(LifecycleStage.DRAFT, Lists.newArrayList());
      if (draftApplications.size() > 1) {
        String joinedProgramIds =
            String.join(
                ", ",
                draftApplications.stream()
                    .map(a -> String.format("%d", a.getProgram().getProgramDefinition().id()))
                    .collect(ImmutableList.toImmutableList()));
        logger.debug(
            String.format(
                "DEBUG LOG ID: 98afa07855eb8e69338b5af13236a6b7. Program"
                    + " Admin Name: %1$s, Duplicate Program Definition"
                    + " ids: %2$s.",
                draftApplications.get(0).getProgram().getProgramDefinition().adminName(),
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
    public abstract ProgramDefinition program();

    public abstract Optional<Instant> latestSubmittedApplicationTime();

    public abstract Optional<StatusDefinitions.Status> latestSubmittedApplicationStatus();

    static Builder builder() {
      return new AutoValue_ApplicantService_ApplicantProgramData.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setProgram(ProgramDefinition v);

      abstract Builder setLatestSubmittedApplicationTime(Optional<Instant> v);

      abstract Builder setLatestSubmittedApplicationStatus(Optional<StatusDefinitions.Status> v);

      abstract ApplicantProgramData build();
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
      ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    if (block.isEnumerator()) {
      return stageEnumeratorUpdates(applicantData, block, updateMetadata, updates);
    } else {
      return stageNormalUpdates(applicantData, block, updateMetadata, updates);
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
      ImmutableSet<Update> updates)
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
          default:
            throw new UnsupportedScalarTypeException(type);
        }
      }
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
    public abstract ImmutableList<ApplicantProgramData> inProgress();

    public abstract ImmutableList<ApplicantProgramData> submitted();

    public abstract ImmutableList<ApplicantProgramData> unapplied();

    static Builder builder() {
      return new AutoValue_ApplicantService_ApplicationPrograms.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setInProgress(ImmutableList<ApplicantProgramData> value);

      abstract Builder setSubmitted(ImmutableList<ApplicantProgramData> value);

      abstract Builder setUnapplied(ImmutableList<ApplicantProgramData> value);

      abstract ApplicationPrograms build();
    }
  }
}
