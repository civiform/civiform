package repository;

import static play.test.Helpers.fakeApplication;

import akka.stream.Materializer;
import io.ebean.DB;
import io.ebean.Database;
import models.LifecycleStage;
import models.Models;
import models.Version;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.Application;
import play.db.ebean.EbeanConfig;
import play.test.Helpers;
import support.ProgramBuilder;
import support.ResourceCreator;
import support.TestConstants;
import support.TestQuestionBank;

public class WithPostgresContainer {

  protected static Application app;

  protected static Materializer mat;

  protected static ResourceCreator resourceCreator;

  protected static TestQuestionBank testQuestionBank = new TestQuestionBank(true);

  @BeforeClass
  public static void startPlay() {
    app = provideApplication();
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

  protected static Application provideApplication() {
    return fakeApplication(TestConstants.TEST_DATABASE_CONFIG);
  }

  protected <T> T instanceOf(Class<T> clazz) {
    return app.injector().instanceOf(clazz);
  }

  @Before
  public void resetTables() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    Database database = DB.getDefault();
    Models.truncate(database);
    Version newActiveVersion = new Version(LifecycleStage.ACTIVE);
    newActiveVersion.save();
  }

  @Before
  public void resetSupportQuestionsCache() {
    testQuestionBank.reset();
  }
}
