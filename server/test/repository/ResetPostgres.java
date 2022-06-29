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
import play.test.Helpers;
import support.ProgramBuilder;
import support.ResourceCreator;
import support.TestQuestionBank;

public class ResetPostgres {

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

  protected <T> T instanceOf(Class<T> clazz) {
    return app.injector().instanceOf(clazz);
  }

  @Before
  public void resetTables() {
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
