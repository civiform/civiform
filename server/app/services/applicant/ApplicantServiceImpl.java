package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.LifecycleStage;
import models.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicationRepository;
import repository.UserRepository;
import repository.VersionRepository;
import services.Path;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.cloud.aws.SimpleEmail;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.ScalarType;

/** Implementation class for ApplicantService interface. */
public final class ApplicantServiceImpl implements ApplicantService {

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
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
  private final Logger logger = LoggerFactory.getLogger(ApplicantServiceImpl.class);

  @Inject
  public ApplicantServiceImpl(
      ApplicationRepository applicationRepository,
      UserRepository userRepository,
      VersionRepository versionRepository,
      ProgramService programService,
      SimpleEmail amazonSESClient,
      Clock clock,
      Config configuration,
      HttpExecutionContext httpExecutionContext) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.userRepository = checkNotNull(userRepository);
    this.versionRepository = checkNotNull(versionRepository);
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

  @Override
  public CompletionStage<Applicant> createApplicant(long userId) {
    Applicant applicant = new Applicant();
    return userRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
  }

  @Override
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        userRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

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

  @Override
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

  @Override
  public ReadOnlyApplicantProgramService getReadOnlyApplicantProgramService(
      Application application, ProgramDefinition programDefinition) {
    return new ReadOnlyApplicantProgramServiceImpl(
        application.getApplicantData(), programDefinition, baseUrl);
  }

  @Override
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
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

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

  @Override
  public CompletionStage<Application> submitApplication(
      long applicantId, long programId, CiviFormProfile submitterProfile) {
    if (submitterProfile.isTrustedIntermediary()) {
      return submitterProfile
          .getAccount()
          .thenComposeAsync(
              account ->
                  submitApplication(applicantId, programId, Optional.of(account.getEmailAddress())),
              httpExecutionContext.current());
    }

    return submitApplication(applicantId, programId, Optional.empty());
  }

  private CompletionStage<Application> submitApplication(
      long applicantId, long programId, Optional<String> submitterEmail) {
    return applicationRepository
        .submitApplication(applicantId, programId, submitterEmail)
        .thenComposeAsync(
            applicationMaybe -> {
              if (applicationMaybe.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new ApplicationSubmissionException(applicantId, programId));
              }
              Application application = applicationMaybe.get();
              String programName = application.getProgram().getProgramDefinition().adminName();
              notifyProgramAdmins(applicantId, programId, application.id, programName);
              if (submitterEmail.isPresent()) {
                notifySubmitter(submitterEmail.get(), applicantId, application.id, programName);
              }
              maybeNotifyApplicant(applicantId, application.id, programName);
              return CompletableFuture.completedFuture(application);
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

  private void notifySubmitter(
      String submitter, long applicantId, long applicationId, String programName) {
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    Optional.empty(), Optional.empty())
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
      amazonSESClient.send(submitter, subject, message);
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

  @Override
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

  @Override
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

  @Override
  public ImmutableList<Application> getAllApplications() {
    return applicationRepository.getAllApplications();
  }

  @Override
  public CompletionStage<RelevantPrograms> relevantProgramsForApplicant(long applicantId) {
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
        applications -> buildRelevantProgramList(activePrograms, applications),
        httpExecutionContext.current());
  }

  private RelevantPrograms buildRelevantProgramList(
      ImmutableList<ProgramDefinition> activePrograms, ImmutableSet<Application> applications) {
    // Sort in descending database ID order. Add to set if it doesn't already exist.
    // If it does, then don't add.
    ImmutableList<Application> sortedApplications =
        applications.stream()
            .sorted(Comparator.comparing(app -> app.id))
            .collect(ImmutableList.toImmutableList())
            .reverse();
    Map<String, Map<LifecycleStage, Application>> mostRecentAppsPerProgram = Maps.newHashMap();
    for (Application application : sortedApplications) {
      String programKey = application.getProgram().getProgramDefinition().adminName();
      Map<LifecycleStage, Application> appsByStage =
          mostRecentAppsPerProgram.getOrDefault(programKey, Maps.newHashMap());
      LifecycleStage applicationStage = application.getLifecycleStage();
      if (appsByStage.containsKey(applicationStage)) {
        // Normally we'd continue onwards.
        Application existingApplicationForStage = appsByStage.get(applicationStage);
        if (applicationStage == LifecycleStage.DRAFT) {
          // If we had to deduplicate, log data for debugging purposes.
          logger.debug(
              String.format(
                  "DEBUG LOG ID: 98afa07855eb8e69338b5af13236a6b7. Program"
                      + " Admin Name: %1$s, Duplicate Program Definition"
                      + " id: %2$s. Original Program Definition id: %3$s",
                  application.getProgram().getProgramDefinition().adminName(),
                  application.getProgram().getProgramDefinition().id(),
                  existingApplicationForStage.id));
        }
        continue;
      }
      appsByStage.put(applicationStage, application);
      mostRecentAppsPerProgram.put(programKey, appsByStage);
    }

    ImmutableMap<String, ProgramDefinition> activeProgramNameLookup =
        activePrograms.stream()
            .collect(ImmutableMap.toImmutableMap(ProgramDefinition::adminName, pdef -> pdef));

    // Loop through all of the applications so that we can make sure a ProgramDefinition
    // is added for already completed / draft programs where the current version may not be visible
    // in the index.
    ImmutableList.Builder<ApplicantProgramData> inProgressPrograms = ImmutableList.builder();
    ImmutableList.Builder<ApplicantProgramData> submittedPrograms = ImmutableList.builder();
    ImmutableList.Builder<ApplicantProgramData> unappliedPrograms = ImmutableList.builder();

    mostRecentAppsPerProgram.forEach(
        (programName, appsByStage) -> {
          Optional<Instant> maybeSubmitTime =
              appsByStage.containsKey(LifecycleStage.ACTIVE)
                  ? Optional.of(appsByStage.get(LifecycleStage.ACTIVE).getSubmitTime())
                  : Optional.empty();
          if (appsByStage.containsKey(LifecycleStage.DRAFT)) {
            // We want to ensure that any generated links points to the version
            // of the program associated with the draft, not the most recent version.
            // As such, we use the program definition associated with the application.
            inProgressPrograms.add(
                ApplicantProgramData.create(
                    appsByStage.get(LifecycleStage.DRAFT).getProgram().getProgramDefinition(),
                    maybeSubmitTime));
          } else if (appsByStage.containsKey(LifecycleStage.ACTIVE)) {
            // Prefer the most recent version of the program. If none exists,
            // fall back on the version used to submit the application.
            ProgramDefinition programDef =
                activeProgramNameLookup.getOrDefault(
                    programName,
                    appsByStage.get(LifecycleStage.ACTIVE).getProgram().getProgramDefinition());
            submittedPrograms.add(ApplicantProgramData.create(programDef, maybeSubmitTime));
          }
        });

    Set<String> missingActivePrograms =
        Sets.difference(activeProgramNameLookup.keySet(), mostRecentAppsPerProgram.keySet());
    missingActivePrograms.forEach(
        programName -> {
          unappliedPrograms.add(
              ApplicantProgramData.create(
                  activeProgramNameLookup.get(programName), Optional.empty()));
        });

    // Ensure each list is ordered by database ID for consistent ordering.
    return RelevantPrograms.builder()
        .setInProgress(
            inProgressPrograms.build().stream()
                .sorted(Comparator.comparing(p -> p.program().id()))
                .collect(ImmutableList.toImmutableList()))
        .setSubmitted(
            submittedPrograms.build().stream()
                .sorted(Comparator.comparing(p -> p.program().id()))
                .collect(ImmutableList.toImmutableList()))
        .setUnapplied(
            unappliedPrograms.build().stream()
                .sorted(Comparator.comparing(p -> p.program().id()))
                .collect(ImmutableList.toImmutableList()))
        .build();
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
    ImmutableMap.Builder<Path, String> failedUpdatesBuilder = ImmutableMap.<Path, String>builder();
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
      return new AutoValue_ApplicantServiceImpl_UpdateMetadata(programId, updatedAt);
    }

    abstract long programId();

    abstract long updatedAt();
  }
}
