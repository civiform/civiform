package repository;

import static play.test.Helpers.fakeApplication;

import akka.stream.Materializer;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import models.Account;
import models.Applicant;
import models.Program;
import models.Question;
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
  public void truncateTables() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    EbeanServer server = Ebean.getServer(config.defaultServer());
    server.truncate(
        Applicant.class, Program.class, Question.class, Account.class, models.Application.class);
  }

  @Before
  public void resetSupportQuestionsCache() {
    testQuestionBank.reset();
  }
}
