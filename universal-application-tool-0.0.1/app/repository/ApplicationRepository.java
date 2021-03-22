package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
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
import play.db.ebean.EbeanConfig;
import services.applicant.ApplicantNotFoundException;
import services.program.ProgramNotFoundException;

public class ApplicationRepository {
  private final ProgramRepository programRepository;
  private final ApplicantRepository applicantRepository;
  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationRepository.class);

  @Inject
  public ApplicationRepository(
      ProgramRepository programRepository,
      ApplicantRepository applicantRepository,
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext) {
    this.programRepository = checkNotNull(programRepository);
    this.applicantRepository = checkNotNull(applicantRepository);
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  /**
   * Submit an application, which will delete any in-progress drafts, obsolete any submitted
   * applications to a program with the same name (to include past versions of the same program),
   * and create a new application in the active state.
   */
  public CompletionStage<Application> submitApplication(Applicant applicant, Program program) {
    return supplyAsync(
        () -> {
          return submitApplicationInternal(applicant, program);
        },
        executionContext.current());
  }

  public CompletionStage<Optional<Application>> submitApplication(
      long applicantId, long programId) {
    return this.perform(
        applicantId,
        programId,
        (ApplicationArguments appArgs) ->
            submitApplicationInternal(appArgs.applicant, appArgs.program));
  }

  private Application submitApplicationInternal(Applicant applicant, Program program) {
    ebeanServer.beginTransaction();
    try {
      List<Application> oldApplications =
          ebeanServer
              .createQuery(Application.class)
              .where()
              .eq("applicant.id", applicant.id)
              .eq("program.name", program.getProgramDefinition().name())
              .findList();
      for (Application application : oldApplications) {
        // Delete any in-progress drafts, and mark obsolete any old applications.
        if (application.getLifecycleStage().equals(LifecycleStage.DRAFT)) {
          application.setLifecycleStage(LifecycleStage.DELETED);
        } else {
          application.setLifecycleStage(LifecycleStage.OBSOLETE);
        }
        application.save();
      }
      Application application = new Application(applicant, program, LifecycleStage.ACTIVE);
      application.save();
      ebeanServer.commitTransaction();
      return application;
    } finally {
      ebeanServer.endTransaction();
    }
  }

  private CompletionStage<Optional<Application>> perform(
      long applicantId, long programId, Function<ApplicationArguments, Application> fn) {
    CompletionStage<Optional<Applicant>> applicantDb =
        applicantRepository.lookupApplicant(applicantId);
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
    ebeanServer.beginTransaction();
    try {
      Optional<Application> existingDraft =
          ebeanServer
              .createQuery(Application.class)
              .where()
              .eq("applicant.id", applicant.id)
              .eq("program.id", program.id)
              .eq("lifecycle_stage", LifecycleStage.DRAFT)
              .findOneOrEmpty();
      Application application =
          existingDraft.orElse(new Application(applicant, program, LifecycleStage.DRAFT));
      application.save();
      ebeanServer.commitTransaction();
      return application;
    } finally {
      ebeanServer.endTransaction();
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
        () -> ebeanServer.find(Application.class).setId(applicationId).findOneOrEmpty(),
        executionContext.current());
  }
}
