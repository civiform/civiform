package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import actions.DemoModeDisabledAction;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.FlashKey;
import controllers.dev.seeding.DevDatabaseSeedTask;
import durablejobs.DurableJobName;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.AsyncCacheApi;
import play.cache.NamedCache;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;
import repository.TransactionManager;
import services.DeploymentType;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import services.settings.SettingsService;
import views.dev.DevToolsPageView;
import views.dev.DevToolsPageViewModel;
import views.dev.DevToolsView;

/** Controller for dev tools. */
@With(DemoModeDisabledAction.class)
public class DevToolsController extends Controller {
  private static final Logger logger = LoggerFactory.getLogger(DevToolsController.class);

  private final DevDatabaseSeedTask devDatabaseSeedTask;
  private final DevToolsView view;
  private final Database database;
  private final SettingsService settingsService;
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;
  private final AsyncCacheApi questionsByVersionCache;
  private final AsyncCacheApi programsByVersionCache;
  private final AsyncCacheApi programCache;
  private final AsyncCacheApi programDefCache;
  private final AsyncCacheApi versionsByProgramCache;
  private final AsyncCacheApi settingsCache;
  private final Clock clock;
  private final TransactionManager transactionManager = new TransactionManager();
  private final FormFactory formFactory;
  private final DevToolsPageView devToolsPageView;
  private final MessagesApi messagesApi;

  @Inject
  public DevToolsController(
      DevDatabaseSeedTask devDatabaseSeedTask,
      DevToolsView view,
      DevToolsPageView devToolsPageView,
      SettingsService settingsService,
      SettingsManifest settingsManifest,
      Clock clock,
      DeploymentType deploymentType,
      FormFactory formFactory,
      MessagesApi messagesApi,
      @NamedCache("version-questions") AsyncCacheApi questionsByVersionCache,
      @NamedCache("version-programs") AsyncCacheApi programsByVersionCache,
      @NamedCache("program") AsyncCacheApi programCache,
      @NamedCache("full-program-definition") AsyncCacheApi programDefCache,
      @NamedCache("program-versions") AsyncCacheApi versionsByProgramCache,
      @NamedCache("civiform-settings") AsyncCacheApi settingsCache) {
    this.devDatabaseSeedTask = checkNotNull(devDatabaseSeedTask);
    this.view = checkNotNull(view);
    this.database = DB.getDefault();
    this.settingsService = checkNotNull(settingsService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.questionsByVersionCache = checkNotNull(questionsByVersionCache);
    this.programsByVersionCache = checkNotNull(programsByVersionCache);
    this.programCache = checkNotNull(programCache);
    this.programDefCache = checkNotNull(programDefCache);
    this.versionsByProgramCache = checkNotNull(versionsByProgramCache);
    this.settingsCache = checkNotNull(settingsCache);
    this.clock = checkNotNull(clock);
    this.deploymentType = checkNotNull(deploymentType);
    this.formFactory = checkNotNull(formFactory);
    this.devToolsPageView = checkNotNull(devToolsPageView);
    this.messagesApi = checkNotNull(messagesApi);
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ImmutableList<String> durableJobOptions =
          ImmutableList.copyOf(DurableJobName.values()).stream()
              .map(DurableJobName::toString)
              .collect(ImmutableList.toImmutableList());
      String csrfToken = play.filters.csrf.CSRF.getToken(request).map(t -> t.value()).orElse("");
      DevToolsPageViewModel model =
          DevToolsPageViewModel.builder()
              .seedProgramsUrl(routes.DevToolsController.seedPrograms().url())
              .seedQuestionsUrl(routes.DevToolsController.seedQuestions().url())
              .clearUrl(routes.DevToolsController.clear().url())
              .clearCacheUrl(routes.DevToolsController.clearCache().url())
              .runDurableJobUrl(routes.DevToolsController.runDurableJob().url())
              .iconsUrl(controllers.dev.routes.IconsController.index().url())
              .homeUrl(controllers.routes.HomeController.index().url())
              .addressToolsUrl(controllers.dev.routes.AddressCheckerController.index().url())
              .sessionProfileUrl(controllers.dev.routes.ProfileController.index().url())
              .sessionDisplayUrl(controllers.dev.routes.SessionDisplayController.index().url())
              .durableJobOptions(durableJobOptions)
              .isDev(deploymentType.isDev())
              .csrfToken(csrfToken)
              .build();
      return ok(devToolsPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }
    return ok(view.render(request, request.flash().get(FlashKey.SUCCESS)));
  }

  public Result seedQuestions() {
    Result result = redirect(routes.DevToolsController.index().url());
    return seedQuestionsInternal()
        ? result.flashing(FlashKey.SUCCESS, "Sample questions seeded")
        : result.flashing(FlashKey.ERROR, "Failed to seed questions");
  }

  public Result seedQuestionsHeadless() {
    return seedQuestionsInternal() ? ok() : internalServerError();
  }

  private boolean seedQuestionsInternal() {
    try {
      devDatabaseSeedTask.seedQuestions();
      return true;
    } catch (RuntimeException ex) {
      logger.error("Failed to seed questions", ex);
      return false;
    }
  }

  public Result seedPrograms(Request request) {
    Result result = redirect(routes.DevToolsController.index().url());
    return seedProgramsInternal(request)
        ? result.flashing(FlashKey.SUCCESS, "The database has been seeded")
        : result.flashing(FlashKey.ERROR, "Failed to seed programs");
  }

  public Result seedProgramsHeadless(Request request) {
    return seedProgramsInternal(request) ? ok() : internalServerError();
  }

  public Result seedApplicationsHeadless(Request request) {
    DynamicForm formData = formFactory.form().bindFromRequest(request);
    String programSlug = formData.get("programSlug");
    int count = Integer.parseInt(formData.get("count"));
    return seedApplicationsInternal(programSlug, count) ? ok() : internalServerError();
  }

  private boolean seedProgramsInternal(Request request) {
    try {
      // TODO: Check whether test program already exists to prevent error.
      ImmutableList<QuestionDefinition> createdSampleQuestions =
          devDatabaseSeedTask.seedQuestions();
      devDatabaseSeedTask.seedProgramCategories();
      devDatabaseSeedTask.insertMinimalSampleProgram(
          createdSampleQuestions,
          messagesApi.preferred(request),
          settingsManifest.getEnumeratorImprovementsEnabled(request));
      devDatabaseSeedTask.insertComprehensiveSampleProgram(
          createdSampleQuestions,
          messagesApi.preferred(request),
          settingsManifest.getEnumeratorImprovementsEnabled(request));

      return true;
    } catch (RuntimeException ex) {
      logger.error("Failed to seed programs.", ex);
      return false;
    }
  }

  private boolean seedApplicationsInternal(String programSlug, int count) {
    try {
      devDatabaseSeedTask.seedApplications(programSlug, count);
      return true;
    } catch (RuntimeException ex) {
      logger.error("Failed to seed applications for program: {}", programSlug, ex);
      return false;
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

  /** Remove all content from all server application tables. */
  public Result clear() {
    Result result = redirect(routes.DevToolsController.index().url());
    return clearInternal()
        ? result.flashing(FlashKey.SUCCESS, "The database has been cleared")
        : result.flashing(FlashKey.ERROR, "Could not clear database");
  }

  /** Remove all content from the program and question tables. */
  public Result clearHeadless() {
    return clearInternal() ? ok() : internalServerError();
  }

  /**
   * Remove all content from all server application tables and clear the responding server's memory
   * caches. Should only be used for testing.
   *
   * <p>Note: This is best-effort and is not a full reset of the DB. Be careful using this in a
   * multitask environment as the deployment DB will have its tables wiped, but only the responding
   * server will have its memory caches cleared.
   *
   * <p>As a practical matter though, clearing the databases doesn't reset the DB table primary-key
   * id generators, so any uncleared caches will only matter in as much as a request can be made for
   * old ids in the servers cache. And even then ebean will fail if data is written with foreign
   * keys that don't exist, etc.
   */
  private boolean clearInternal() {
    try {
      logger.warn("Beginning clearing of data. Clearing memory caches.");
      clearCacheIfEnabled();
      logger.warn("Clearing database tables.");
      truncateTables();
      logger.warn("Done clearing data.");

      return true;
    } catch (RuntimeException ex) {
      logger.error("Failed to clear cache or tables.", ex);
      return false;
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
   * <p>Note: this is possibly not safe to do in a multitask environment depending on the desired
   * outcome as only a singular task will respond and clear its caches, while others will not.
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

    if (settingsManifest.getSettingsCacheEnabled()) {
      settingsCache.removeAll().toCompletableFuture().join();
    }
  }

  /**
   * Removes rows from all tables but otherwise doesn't modify them.
   *
   * <p>Row id sequence counters are not reset for instance.
   */
  private void truncateTables() {
    transactionManager.execute(
        () -> {
          Models.truncate(database);
          VersionModel newActiveVersion = new VersionModel(LifecycleStage.ACTIVE);
          newActiveVersion.save();
          settingsService.migrateConfigValuesToSettingsGroup();
        });
  }
}
