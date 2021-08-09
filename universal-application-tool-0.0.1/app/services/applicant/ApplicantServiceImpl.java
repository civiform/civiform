package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicationRepository;
import repository.UserRepository;
import services.Path;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.aws.SimpleEmail;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.ScalarType;

/** Implementation class for ApplicantService interface. */
public class ApplicantServiceImpl implements ApplicantService {

  private static final String STAGING_PROGRAM_ADMIN_NOTIFICATION_MAILING_LIST =
      "seattle-civiform-program-admins-notify@google.com";
  private static final String STAGING_TI_NOTIFICATION_MAILING_LIST =
      "seattle-civiform-trusted-intermediaries-notify@google.com";
  private static final String STAGING_APPLICANT_NOTIFICATION_MAILING_LIST =
      "seattle-civiform-applicants-notify@google.com";

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final ProgramService programService;
  private final SimpleEmail amazonSESClient;
  private final Clock clock;
  private final String baseUrl;
  private final boolean isStaging;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ApplicantServiceImpl(
      ApplicationRepository applicationRepository,
      UserRepository userRepository,
      ProgramService programService,
      SimpleEmail amazonSESClient,
      Clock clock,
      Config configuration,
      HttpExecutionContext httpExecutionContext) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.userRepository = checkNotNull(userRepository);
    this.programService = checkNotNull(programService);
    this.amazonSESClient = checkNotNull(amazonSESClient);
    this.clock = checkNotNull(clock);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.isStaging = URI.create(baseUrl).getHost().equals("staging.seattle.civiform.com");
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
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
              try {
                stageUpdates(
                    applicant.getApplicantData(), blockBeforeUpdate, updateMetadata, updates);
              } catch (UnsupportedScalarTypeException | PathNotInBlockException e) {
                return CompletableFuture.failedFuture(e);
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition, baseUrl);

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

  @Override
  public CompletionStage<ImmutableMap<LifecycleStage, ImmutableList<ProgramDefinition>>>
      relevantPrograms(long applicantId) {
    return userRepository.programsForApplicant(applicantId);
  }

  private void notifyProgramAdmins(
      long applicantId, long programId, long applicationId, String programName) {
    String viewLink =
        baseUrl
            + controllers.admin.routes.AdminApplicationController.show(programId, applicationId)
                .url();
    String subject = String.format("Applicant %d submitted a new application", applicantId);
    String message =
        String.format(
            "Applicant %d submitted a new application to program %s.\nView the application at %s.",
            applicantId, programName, viewLink);
    if (isStaging) {
      amazonSESClient.send(STAGING_PROGRAM_ADMIN_NOTIFICATION_MAILING_LIST, subject, message);
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
      amazonSESClient.send(STAGING_TI_NOTIFICATION_MAILING_LIST, subject, message);
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
      amazonSESClient.send(STAGING_APPLICANT_NOTIFICATION_MAILING_LIST, subject, message);
    } else {
      amazonSESClient.send(email.get(), subject, message);
    }
  }

  @Override
  public CompletionStage<String> getName(long applicantId) {
    return userRepository
        .lookupApplicant(applicantId)
        .thenApplyAsync(
            applicant -> {
              if (applicant.isEmpty()) {
                return "<Anonymous Applicant>";
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

  /**
   * In-place update of {@link ApplicantData}. Adds program id and timestamp metadata with updates.
   *
   * @throws PathNotInBlockException if there are updates for questions that aren't in the block.
   * @throws UnsupportedScalarTypeException if there are updates for unsupported scalar types.
   */
  private void stageUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    if (block.isEnumerator()) {
      stageEnumeratorUpdates(applicantData, block, updateMetadata, updates);
    } else {
      stageNormalUpdates(applicantData, block, updateMetadata, updates);
    }
  }

  /**
   * Stage updates for an enumerator.
   *
   * @throws PathNotInBlockException for updates that aren't {@link Scalar#ENTITY_NAME}, or {@link
   *     Scalar#DELETE_ENTITY}.
   */
  private void stageEnumeratorUpdates(
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
   * @throws PathNotInBlockException if there are updates for questions that aren't in the block.
   * @throws UnsupportedScalarTypeException if there are updates for unsupported scalar types.
   */
  private void stageNormalUpdates(
      ApplicantData applicantData,
      Block block,
      UpdateMetadata updateMetadata,
      ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    ArrayList<Path> visitedPaths = new ArrayList<>();
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
          case DATE:
            applicantData.putDate(currentPath, update.value());
            break;
          case LIST_OF_STRINGS:
          case STRING:
            applicantData.putString(currentPath, update.value());
            break;
          case LONG:
            applicantData.putLong(currentPath, update.value());
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
