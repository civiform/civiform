package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.ebean.DB;
import io.ebean.Database;
import models.LifecycleStage;
import models.Models;
import models.Version;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
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

  private final VersionRepository versionRepository;
  private final SettingsManifest settingsManifest;
  private final SyncCacheApi questionsByVersionCache;
  private final SyncCacheApi programsByVersionCache;

  @Inject
  public DevDatabaseSeedController(
      DevDatabaseSeedTask devDatabaseSeedTask,
      DatabaseSeedView view,
      QuestionService questionService,
      ProgramService programService,
      SettingsService settingsService,
      DeploymentType deploymentType,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      @NamedCache("version-questions") SyncCacheApi questionsByVersionCache,
      @NamedCache("version-programs") SyncCacheApi programsByVersionCache) {
    this.devDatabaseSeedTask = checkNotNull(devDatabaseSeedTask);
    this.view = checkNotNull(view);
    this.database = DB.getDefault();
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
    this.settingsService = checkNotNull(settingsService);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
    this.versionRepository = checkNotNull(versionRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.questionsByVersionCache = checkNotNull(questionsByVersionCache);
    this.programsByVersionCache = checkNotNull(programsByVersionCache);
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (!isDevOrStaging) {
      return notFound();
    }
    ActiveAndDraftPrograms activeAndDraftPrograms = programService.getActiveAndDraftPrograms();
    ImmutableList<QuestionDefinition> questionDefinitions =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join().getAllQuestions();
    return ok(
        view.render(
            request, activeAndDraftPrograms, questionDefinitions, request.flash().get("success")));
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

    devDatabaseSeedTask.insertMinimalSampleProgram(createdSampleQuestions);
    devDatabaseSeedTask.insertComprehensiveSampleProgram(createdSampleQuestions);
    return redirect(routes.DevDatabaseSeedController.index().url())
        .flashing("success", "The database has been seeded");
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    if (!isDevOrStaging) {
      return notFound();
    }
    if (settingsManifest.getVersionCacheEnabled()) {
      clearCache();
    }
    resetTables();
    return redirect(routes.DevDatabaseSeedController.index().url())
        .flashing("success", "The database has been cleared");
  }

  /** Remove all content from the cache. */
  public void clearCache() {
    if (!isDevOrStaging) return;
    Version activeVersion = versionRepository.getActiveVersion();
    clearVersionCache(programsByVersionCache, activeVersion);
    clearVersionCache(questionsByVersionCache, activeVersion);
  }

  /**
   * Remove all content from the version cache, using the active version as the maximum key to
   * clear.
   */
  private void clearVersionCache(SyncCacheApi cache, Version activeVersion) {
    for (int num = 1; num <= activeVersion.id; num++) {
      if (cache.get(String.valueOf(num)).isPresent()) {
        cache.remove(String.valueOf(num));
      }
    }
  }

  // Create a date question definition with the given name and questionText. We currently create
  // multiple date questions in a single program for testing.

  private void resetTables() {
    Models.truncate(database);
    Version newActiveVersion = new Version(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    settingsService.migrateConfigValuesToSettingsGroup();
  }
}
