package repository;

import static play.test.Helpers.fakeApplication;

import io.ebean.DB;
import io.ebean.Database;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import models.LifecycleStage;
import models.Models;
import models.VersionModel;
import org.apache.pekko.stream.Materializer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.Application;
import play.api.inject.BindingKey;
import play.test.Helpers;
import services.settings.SettingsService;
import support.ProgramBuilder;
import support.ResourceCreator;
import support.TestQuestionBank;

public class ResetPostgres {

  protected Clock testClock =
      Clock.fixed(Instant.parse("2021-01-15T00:00:00.00Z"), ZoneId.systemDefault());

  protected static Application app;

  protected static Materializer mat;

  protected static ResourceCreator resourceCreator;

  protected static TestQuestionBank testQuestionBank = new TestQuestionBank(true);

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

  @Before
  public void resetTables() {
    Database database = DB.getDefault();
    Models.truncate(database);
    VersionModel newActiveVersion = new VersionModel(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    instanceOf(SettingsService.class).migrateConfigValuesToSettingsGroup();
  }

  @Before
  public void resetSupportQuestionsCache() {
    testQuestionBank.reset();
  }
}
