package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.applicant.exception.ApplicantNotFoundException;
import services.program.ProgramNotFoundException;

/**
 * ApplicationRepository performs complicated operations on {@link Application} that often involve
 * other EBean models or asynchronous handling.
 */
public class ApplicationRepository {
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationRepository.class);

  @Inject
  public ApplicationRepository(
      ProgramRepository programRepository,
      UserRepository userRepository,
      DatabaseExecutionContext executionContext) {
    this.programRepository = checkNotNull(programRepository);
    this.userRepository = checkNotNull(userRepository);
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  /**
   * Submit an application, which will delete any in-progress drafts, obsolete any submitted
   * applications to a program with the same name (to include past versions of the same program),
   * and create a new application in the active state.
   */
  public CompletionStage<Application> submitApplication(
      Applicant applicant, Program program, Optional<String> submitterEmail) {
    return supplyAsync(
        () -> {
          return submitApplicationInternal(applicant, program, submitterEmail);
        },
        executionContext.current());
  }

  public CompletionStage<Optional<Application>> submitApplication(
      long applicantId, long programId, Optional<String> submitterEmail) {
    return this.perform(
        applicantId,
        programId,
        (ApplicationArguments appArgs) ->
            submitApplicationInternal(appArgs.applicant, appArgs.program, submitterEmail));
  }

  private Application submitApplicationInternal(
      Applicant applicant, Program program, Optional<String> submitterEmail) {
    database.beginTransaction();
    try {
      List<Application> oldApplications =
          database
              .createQuery(Application.class)
              .where()
              .eq("applicant.id", applicant.id)
              .eq("program.name", program.getProgramDefinition().adminName())
              .findList();
      Optional<Application> completedApplication = Optional.empty();
      for (Application application : oldApplications) {
        // Delete any in-progress drafts, and mark obsolete any old applications.
        if (application.getLifecycleStage().equals(LifecycleStage.DRAFT)) {
          application.setLifecycleStage(LifecycleStage.ACTIVE);
          application.setSubmitTimeToNow();
          completedApplication = Optional.of(application);
        } else {
          application.setLifecycleStage(LifecycleStage.OBSOLETE);
        }
        application.save();
      }
      Application application =
          completedApplication.orElse(new Application(applicant, program, LifecycleStage.ACTIVE));
      application.setSubmitTimeToNow();

      if (submitterEmail.isPresent()) {
        application.setSubmitterEmail(submitterEmail.get());
      }

      application.save();
      database.commitTransaction();
      return application;
    } finally {
      database.endTransaction();
    }
  }

  private CompletionStage<Optional<Application>> perform(
      long applicantId, long programId, Function<ApplicationArguments, Application> fn) {
    CompletionStage<Optional<Applicant>> applicantDb = userRepository.lookupApplicant(applicantId);
    CompletionStage<Optional<Program>> programDb = programRepository.lookupProgram(programId);
    return applicantDb
        .thenCombineAsync(
            programDb,
            (applicantMaybe, programMaybe) -> {
              if (applicantMaybe.isEmpty()) {
                throw new RuntimeException(new ApplicantNotFoundException(applicantId));
              }
              if (programMaybe.isEmpty()) {
                throw new RuntimeException(new ProgramNotFoundException(programId));
              }
              return new ApplicationArguments(programMaybe.get(), applicantMaybe.get());
            })
        .thenApplyAsync(fn)
        .thenApplyAsync(application -> Optional.of(application))
        .exceptionally(
            exception -> {
              LOG.error(exception.toString());
              exception.printStackTrace();
              return Optional.empty();
            });
  }

  public ImmutableList<Application> getAllApplications() {
    return ImmutableList.copyOf(database.find(Application.class).findList());
  }

  // Need to transmit both arguments to submitApplication through the CompletionStage pipeline.
  // Not useful in the API, not needed more broadly.
  private static class ApplicationArguments {
    public Program program;
    public Applicant applicant;

    public ApplicationArguments(Program program, Applicant applicant) {
      this.program = program;
      this.applicant = applicant;
    }
  }

  private Application createOrUpdateDraftApplicationInternal(Applicant applicant, Program program) {
    database.beginTransaction();
    try {
      Optional<Application> existingDraft =
          database
              .createQuery(Application.class)
              .where()
              .eq("applicant.id", applicant.id)
              .eq("program.id", program.id)
              .eq("lifecycle_stage", LifecycleStage.DRAFT)
              .findOneOrEmpty();
      Application application =
          existingDraft.orElse(new Application(applicant, program, LifecycleStage.DRAFT));
      application.setApplicantData(applicant.getApplicantData());
      application.save();
      database.commitTransaction();
      return application;
    } finally {
      database.endTransaction();
    }
  }

  /**
   * Create a draft application for the specified program. Update the draft application if one
   * already exists.
   */
  public CompletionStage<Application> createOrUpdateDraft(Applicant applicant, Program program) {
    return supplyAsync(
        () -> {
          return createOrUpdateDraftApplicationInternal(applicant, program);
        },
        executionContext.current());
  }

  public CompletionStage<Optional<Application>> createOrUpdateDraft(
      long applicantId, long programId) {
    return this.perform(
        applicantId,
        programId,
        (ApplicationArguments appArgs) ->
            createOrUpdateDraftApplicationInternal(appArgs.applicant, appArgs.program));
  }

  public CompletionStage<Optional<Application>> getApplication(long applicationId) {
    return supplyAsync(
        () -> database.find(Application.class).setId(applicationId).findOneOrEmpty(),
        executionContext.current());
  }
}
