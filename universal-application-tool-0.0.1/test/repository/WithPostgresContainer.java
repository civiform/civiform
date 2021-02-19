package repository;

import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import models.Applicant;
import models.Person;
import models.Program;
import models.Question;
import org.junit.Before;
import play.Application;
import play.db.ebean.EbeanConfig;
import play.test.WithApplication;

public class WithPostgresContainer extends WithApplication {

  @Before
  public void truncateTables() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    EbeanServer server = Ebean.getServer(config.defaultServer());
    server.truncate(Applicant.class, Person.class, Program.class, Question.class);
  }

  protected Application provideApplication() {
    return fakeApplication(
        ImmutableMap.of(
            "db.default.driver",
            "org.testcontainers.jdbc.ContainerDatabaseDriver",
            "db.default.url",
            /* This is a magic string.  The components of it are
             * jdbc: the standard java database connection uri scheme
             * tc: Testcontainers - the tool that starts a new container per test.
             * postgresql: which container to start
             * 9.6.8: which version of postgres to start
             * ///: hostless URI scheme - anything here would be ignored
             * databasename: the name of the db to connect to - any string is okay.
             */
            "jdbc:tc:postgresql:12.5:///databasename",
            "play.evolutions.db.default.enabled ",
            "true"));
  }
}
