package repository;

import static play.test.Helpers.fakeApplication;

import akka.stream.Materializer;
import com.google.common.collect.ImmutableMap;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import models.Applicant;
import models.Person;
import models.Program;
import models.Question;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.Application;
import play.db.ebean.EbeanConfig;
import play.test.Helpers;
import support.ResourceFabricator;

public class WithPostgresContainer {

  protected static Application app;

  protected static Materializer mat;

  protected static ResourceFabricator resourceFabricator;

  @BeforeClass
  public static void startPlay() {
    app = provideApplication();
    resourceFabricator = new ResourceFabricator(app.injector());
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
    return fakeApplication(
        ImmutableMap.of(
            "db.default.driver",
            "org.testcontainers.jdbc.ContainerDatabaseDriver",
            "db.default.url",
            /* This is a magic string.  The components of it are
             * jdbc: the standard java database connection uri scheme
             * tc: Testcontainers - the tool that starts a new container per test.
             * postgresql: which container to start
             * 12.5: which version of postgres to start
             * ///: hostless URI scheme - anything here would be ignored
             * databasename: the name of the db to connect to - any string is okay.
             */
            "jdbc:tc:postgresql:12.5:///databasename",
            "play.evolutions.db.default.enabled ",
            "true"));
  }

  protected <T> T instanceOf(Class<T> clazz) {
    return app.injector().instanceOf(clazz);
  }

  protected ResourceFabricator resourceFabricator() {
    return resourceFabricator;
  }

  @Before
  public void truncateTables() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    EbeanServer server = Ebean.getServer(config.defaultServer());
    server.truncate(Applicant.class, Person.class, Program.class, Question.class);
  }
}
