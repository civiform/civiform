package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.FlashKey;
import controllers.dev.seeding.DevDatabaseSeedTask;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import models.JobType;
import models.LifecycleStage;
import models.Models;
import models.PersistedDurableJobModel;
import models.VersionModel;
import play.cache.AsyncCacheApi;
import play.cache.NamedCache;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import services.settings.SettingsService;
import views.dev.DevToolsView;

/** Controller for dev tools. */
public class DevToolsController extends Controller {

  private final DevDatabaseSeedTask devDatabaseSeedTask;
  private final DevToolsView view;
  private final Database database;
  private final QuestionService questionService;
  private final ProgramService programService;
  private final SettingsService settingsService;
  private final SettingsManifest settingsManifest;
  private final AsyncCacheApi questionsByVersionCache;
  private final AsyncCacheApi programsByVersionCache;
  private final AsyncCacheApi programCache;
  private final AsyncCacheApi programDefCache;
  private final AsyncCacheApi versionsByProgramCache;
  private final Clock clock;

  @Inject
  public DevToolsController(
      DevDatabaseSeedTask devDatabaseSeedTask,
      DevToolsView view,
      QuestionService questionService,
      ProgramService programService,
      SettingsService settingsService,
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
    return ok(view.render(request, request.flash().get(FlashKey.SUCCESS)));
  }

  public Result data(Request request) {
    ActiveAndDraftPrograms activeAndDraftPrograms = programService.getActiveAndDraftPrograms();
    ImmutableList<QuestionDefinition> questionDefinitions =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join().getAllQuestions();
    return ok(view.renderSeedDataView(request, activeAndDraftPrograms, questionDefinitions));
  }

  public Result seedQuestions() {
    devDatabaseSeedTask.seedQuestions();

    return redirect(routes.DevToolsController.index().url())
        .flashing(FlashKey.SUCCESS, "Sample questions seeded");
  }

  public Result seedQuestionsHeadless() {
    try {
      devDatabaseSeedTask.seedQuestions();
      return ok();
    } catch (RuntimeException ex) {
      return internalServerError();
    }
  }

  public Result seedPrograms() {
    // TODO: Check whether test program already exists to prevent error.
    ImmutableList<QuestionDefinition> createdSampleQuestions = devDatabaseSeedTask.seedQuestions();

    devDatabaseSeedTask.seedProgramCategories();
    devDatabaseSeedTask.insertMinimalSampleProgram(createdSampleQuestions);
    devDatabaseSeedTask.insertComprehensiveSampleProgram(createdSampleQuestions);
    return redirect(routes.DevToolsController.index().url())
        .flashing(FlashKey.SUCCESS, "The database has been seeded");
  }

  public Result seedProgramsHeadless() {
    try {
      // TODO: Check whether test program already exists to prevent error.
      ImmutableList<QuestionDefinition> createdSampleQuestions =
          devDatabaseSeedTask.seedQuestions();

      devDatabaseSeedTask.seedProgramCategories();
      devDatabaseSeedTask.insertMinimalSampleProgram(createdSampleQuestions);
      devDatabaseSeedTask.insertComprehensiveSampleProgram(createdSampleQuestions);

      return ok();
    } catch (RuntimeException ex) {
      return internalServerError();
    }
  }

  public Result runDurableJob(Request request) throws InterruptedException {
    String jobName = request.body().asFormUrlEncoded().get("durableJobSelect")[0];
    // I think there's currently a bug where the job runner interprets the timestamps as local
    // times rather than UTC. So set it to yesterday to ensure it runs.
    Instant runTime =
        LocalDateTime.now(clock)
            .minus(1, ChronoUnit.DAYS)
            .toInstant(clock.getZone().getRules().getOffset(Instant.now()));

    // Job types run on demand should be set to recurring in order to be picked up by pekko
    // and run dynamically.
    PersistedDurableJobModel job =
        new PersistedDurableJobModel(jobName, JobType.RECURRING, runTime);
    Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE);
    job.save(transaction);
    transaction.commit();
    return ok(String.format("Added one-time run of %s", jobName));
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    clearCacheIfEnabled();
    resetTables();
    return redirect(routes.DevToolsController.index().url())
        .flashing(FlashKey.SUCCESS, "The database has been cleared");
  }

  /** Remove all content from the program and question tables. */
  public Result clearHeadless() {
    try {
      clearCacheIfEnabled();
      resetTables();
      return ok();
    } catch (RuntimeException ex) {
      return internalServerError();
    }
  }

  /** Remove all content from the cache. */
  public Result clearCache() {
    if (!settingsManifest.getVersionCacheEnabled() && !settingsManifest.getProgramCacheEnabled()) {
      return redirect(routes.DevToolsController.index().url())
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

  private void resetTables() {
    Models.truncate(database);
    VersionModel newActiveVersion = new VersionModel(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    settingsService.migrateConfigValuesToSettingsGroup();
  }
}
