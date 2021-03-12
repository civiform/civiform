package repository;

import static play.test.Helpers.fakeApplication;

import akka.stream.Materializer;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import models.Applicant;
import models.Program;
import models.Question;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.Application;
import play.db.ebean.EbeanConfig;
import play.test.Helpers;
import support.Questions;
import support.ResourceCreator;
import support.TestConstants;

public class WithPostgresContainer {

  protected static Application app;

  protected static Materializer mat;

  protected static ResourceCreator resourceCreator;

  @BeforeClass
  public static void startPlay() {
    app = provideApplication();
    resourceCreator = new ResourceCreator(app.injector());
    Helpers.start(app);
    mat = app.asScala().materializer();
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

  protected ResourceCreator resourceCreator() {
    return resourceCreator;
  }

  @Before
  public void truncateTables() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    EbeanServer server = Ebean.getServer(config.defaultServer());
    server.truncate(Applicant.class, Program.class, Question.class);
  }

  @Before
  public void resetSupportQuestionsCache() {
    Questions.reset();
  }
}
