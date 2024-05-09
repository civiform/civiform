package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import models.LifecycleStage;
import models.Models;
import models.PersistedDurableJobModel;
import models.VersionModel;
import play.cache.AsyncCacheApi;
import play.cache.NamedCache;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.DeploymentType;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import services.settings.SettingsService;
import views.dev.DatabaseSeedView;

/** Controller for seeding the development database with test content. */
public class DevDatabaseSeedController extends Controller {

  private final DevDatabaseSeedTask devDatabaseSeedTask;
  private final DatabaseSeedView view;
  private final Database database;
  private final QuestionService questionService;
  private final ProgramService programService;
  private final SettingsService settingsService;
  private final boolean isDevOrStaging;
  private final SettingsManifest settingsManifest;
  private final AsyncCacheApi questionsByVersionCache;
  private final AsyncCacheApi programsByVersionCache;
  private final AsyncCacheApi programCache;
  private final AsyncCacheApi programDefCache;
  private final AsyncCacheApi versionsByProgramCache;
  private final Clock clock;

  @Inject
  public DevDatabaseSeedController(
      DevDatabaseSeedTask devDatabaseSeedTask,
      DatabaseSeedView view,
      QuestionService questionService,
      ProgramService programService,
      SettingsService settingsService,
      DeploymentType deploymentType,
      SettingsManifest settingsManifest,
      Clock clock,
      @NamedCache("version-questions") AsyncCacheApi questionsByVersionCache,
      @NamedCache("version-programs") AsyncCacheApi programsByVersionCache,
      @NamedCache("program") AsyncCacheApi programCache,
      @NamedCache("full-program-definition") AsyncCacheApi programDefCache,
      @NamedCache("program-versions") AsyncCacheApi versionsByProgramCache) {
    this.devDatabaseSeedTask = checkNotNull(devDatabaseSeedTask);
    this.view = checkNotNull(view);
    this.database = DB.getDefault();
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
    this.settingsService = checkNotNull(settingsService);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
    this.settingsManifest = checkNotNull(settingsManifest);
    this.questionsByVersionCache = checkNotNull(questionsByVersionCache);
    this.programsByVersionCache = checkNotNull(programsByVersionCache);
    this.programCache = checkNotNull(programCache);
    this.programDefCache = checkNotNull(programDefCache);
    this.versionsByProgramCache = checkNotNull(versionsByProgramCache);
    this.clock = checkNotNull(clock);
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (!isDevOrStaging) {
      return notFound();
    }
    return ok(view.render(request, request.flash().get("success")));
  }

  public Result data(Request request) {
    if (!isDevOrStaging) {
      return notFound();
    }
    ActiveAndDraftPrograms activeAndDraftPrograms = programService.getActiveAndDraftPrograms();
    ImmutableList<QuestionDefinition> questionDefinitions =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join().getAllQuestions();
    return ok(view.seedDataView(request, activeAndDraftPrograms, questionDefinitions));
  }

  public Result seedQuestions() {
    if (!isDevOrStaging) {
      return notFound();
    }

    devDatabaseSeedTask.seedQuestions();

    return redirect(routes.DevDatabaseSeedController.index().url())
        .flashing("success", "Sample questions seeded");
  }

  public Result seedPrograms() {
    // TODO: Check whether test program already exists to prevent error.
    if (!isDevOrStaging) {
      return notFound();
    }
    ImmutableList<QuestionDefinition> createdSampleQuestions = devDatabaseSeedTask.seedQuestions();

    devDatabaseSeedTask.seedProgramCategories();
    devDatabaseSeedTask.insertMinimalSampleProgram(createdSampleQuestions);
    devDatabaseSeedTask.insertComprehensiveSampleProgram(createdSampleQuestions);
    return redirect(routes.DevDatabaseSeedController.index().url())
        .flashing("success", "The database has been seeded");
  }

  public Result runDurableJob(Request request) throws InterruptedException {
    String jobName = request.body().asFormUrlEncoded().get("durableJobSelect")[0];
    // I think there's currently a bug where the job runner interprets the timestamps as local
    // times rather than UTC. So set it to yesterday to ensure it runs.
    Instant runTime =
        LocalDateTime.now(clock)
            .minus(1, ChronoUnit.DAYS)
            .toInstant(clock.getZone().getRules().getOffset(Instant.now()));
    PersistedDurableJobModel job = new PersistedDurableJobModel(jobName, runTime);
    Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE);
    job.save(transaction);
    transaction.commit();
    return ok(String.format("Added one-time run of %s", jobName));
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    if (!isDevOrStaging) {
      return notFound();
    }
    clearCacheIfEnabled();
    resetTables();
    return redirect(routes.DevDatabaseSeedController.index().url())
        .flashing("success", "The database has been cleared");
  }

  /** Remove all content from the cache. */
  public Result clearCache() {
    if (!isDevOrStaging) {
      return notFound();
    }
    if (!settingsManifest.getVersionCacheEnabled() && !settingsManifest.getProgramCacheEnabled()) {
      return redirect(routes.DevDatabaseSeedController.index().url())
          .flashing(
              "warning",
              "The cache is not enabled, so no cache was cleared. To enable caching, set"
                  + " VERSION_CACHE_ENABLED or PROGRAM_CACHE_ENABLED to true.");
    }
    clearCacheIfEnabled();
    // We don't redirect to the index page, since that would reset the cache.
    return ok("The cache has been cleared");
  }

  /**
   * Clear cache if it is enabled in settings.
   *
   * <p>Note: this is not safe to do in most deployed instances, because there may be multiple
   * tasks, but we assume all dev instances only have one task.
   */
  private void clearCacheIfEnabled() {
    if (!isDevOrStaging) {
      return;
    }
    if (settingsManifest.getVersionCacheEnabled()) {
      programsByVersionCache.removeAll().toCompletableFuture().join();
      questionsByVersionCache.removeAll().toCompletableFuture().join();
    }
    if (settingsManifest.getProgramCacheEnabled()) {
      programCache.removeAll().toCompletableFuture().join();
      versionsByProgramCache.removeAll().toCompletableFuture().join();
    }
    if (settingsManifest.getQuestionCacheEnabled()) {
      programDefCache.removeAll().toCompletableFuture().join();
    }
  }

  // Create a date question definition with the given name and questionText. We currently create
  // multiple date questions in a single program for testing.

  private void resetTables() {
    Models.truncate(database);
    VersionModel newActiveVersion = new VersionModel(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    settingsService.migrateConfigValuesToSettingsGroup();
  }
}
