package repository;

import static play.test.Helpers.fakeApplication;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import models.LifecycleStage;
import models.VersionModel;
import org.apache.pekko.stream.Materializer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.Application;
import play.api.inject.BindingKey;
import play.test.Helpers;
import services.settings.SettingsService;
import support.ProgramBuilder;
import support.ResourceCreator;
import support.SqlStatementCounter;
import support.TestQuestionBank;

public class ResetPostgres {

  protected Clock testClock =
      Clock.fixed(Instant.parse("2021-01-15T00:00:00.00Z"), ZoneId.systemDefault());

  protected static Application app;

  protected static Materializer mat;

  protected static ResourceCreator resourceCreator;

  protected static TestQuestionBank testQuestionBank = new TestQuestionBank(true);

  private final SqlStatementCounter sqlStatementCounter = new SqlStatementCounter();

  @BeforeClass
  public static void startPlay() {
    app = fakeApplication();
    resourceCreator = new ResourceCreator(app.injector());
    Helpers.start(app);
    mat = app.asScala().materializer();
    ProgramBuilder.setInjector(app.injector());
  }

  @AfterClass
  public static void stopPlay() {
    if (app != null) {
      // Stop settings a test stored from leaking into the next test class.
      resourceCreator.clearCiviFormSettings();
      Helpers.stop(app);
      app = null;
    }
  }

  protected <T> T instanceOf(BindingKey<T> key) {
    return app.injector().instanceOf(key);
  }

  protected <T> T instanceOf(Class<T> clazz) {
    return app.injector().instanceOf(clazz);
  }

  /**
   * Counts SQL statements, split into reads and writes, run since the previous call. Counting
   * starts on the first call, so call this once before the code under test. Statements in a
   * transaction begun before that first call are not counted.
   *
   * <p>See {@link SqlStatementCounter} for what is counted.
   */
  protected SqlStatementCounter.SqlCounts sqlStatementsSinceLastCount() {
    sqlStatementCounter.start();
    return sqlStatementCounter.collect();
  }

  @After
  public void stopCountingStatements() {
    sqlStatementCounter.stop();
  }

  @Before
  public void resetTables() {
    resourceCreator.truncateTables();
    VersionModel newActiveVersion = new VersionModel(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    SettingsService settingsService = instanceOf(SettingsService.class);
    settingsService.migrateConfigValuesToSettingsGroup();
    // Cache the settings, as they are once a server has started, so that loading them doesn't
    // count towards the SQL statements of the code under test.
    settingsService.loadSettings().toCompletableFuture().join();
  }

  @Before
  public void resetSupportQuestionsCache() {
    testQuestionBank.reset();
  }
}
