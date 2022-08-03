package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.ExpressionList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
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
  private static final Logger logger = LoggerFactory.getLogger(ApplicationRepository.class);

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
   * Pages through all submitted applications calling the provided consumer function with each one.
   * Program association is eager loaded.
   */
  public void forEachSubmittedApplication(Consumer<Application> fn) {
    database
        .find(Application.class)
        .fetch("program")
        .where()
        .in("lifecycle_stage", ImmutableList.of(LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
        .findEach(fn);
  }

  /**
   * Submit an application, which will delete any in-progress drafts, obsolete any submitted
   * applications to a program with the same name (to include past versions of the same program),
   * and create a new application in the active state.
   */
  public CompletionStage<Application> submitApplication(
      Applicant applicant, Program program, Optional<String> submitterEmail) {
    return supplyAsync(
        () -> submitApplicationInternal(applicant, program, submitterEmail),
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
      ImmutableList<Application> drafts =
          oldApplications.stream()
              .filter(app -> app.getLifecycleStage().equals(LifecycleStage.DRAFT))
              .sorted((app1, app2) -> app1.getCreateTime().isAfter(app2.getCreateTime()) ? -1 : 1)
              .collect(ImmutableList.toImmutableList());
      if (drafts.size() > 1) {
        // TODO(#3045): Revert this after the issue is fixed in prod. Desired behavior is to fail.
        logger.error(
            String.format(
                "Found more than one DRAFT application for applicant %d, program %d.",
                applicant.id, program.id));
      }

      Application application =
          drafts.isEmpty()
              ? new Application(applicant, program, LifecycleStage.ACTIVE)
              : drafts.get(0);
      application.setLifecycleStage(LifecycleStage.ACTIVE);
      application.setSubmitTimeToNow();
      if (submitterEmail.isPresent()) {
        application.setSubmitterEmail(submitterEmail.get());
      }
      application.save();

      for (Application app : oldApplications) {
        if (application.id.equals(app.id)
            || app.getLifecycleStage().equals(LifecycleStage.OBSOLETE)) {
          continue;
        }
        app.setLifecycleStage(LifecycleStage.OBSOLETE);
        app.save();
      }
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
              logger.error(exception.toString());
              exception.printStackTrace();
              return Optional.empty();
            });
  }

  /**
   * Returns all applications submitted within the provided time range. Results are returned in the
   * order that the applications were created.
   */
  public ImmutableList<Application> getApplications(TimeFilter submitTimeFilter) {
    ExpressionList<Application> query =
        database
            .find(Application.class)
            .fetch("program")
            .fetch("applicant.account")
            .orderBy("id")
            .where();
    if (submitTimeFilter.fromTime().isPresent()) {
      query = query.where().ge("submit_time", submitTimeFilter.fromTime().get());
    }
    if (submitTimeFilter.untilTime().isPresent()) {
      query = query.where().lt("submit_time", submitTimeFilter.untilTime().get());
    }
    return ImmutableList.copyOf(query.findList());
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
