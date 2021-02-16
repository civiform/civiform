package repository;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import models.Applicant;
import models.Person;
import models.Program;
import models.Question;
import org.junit.Before;
import play.db.ebean.EbeanConfig;

public class WithResettingPostgresContainer extends WithPostgresContainer {
  @Before
  public void clearDatabase() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    EbeanServer server = Ebean.getServer(config.defaultServer());
    server.truncate(Applicant.class, Person.class, Program.class, Question.class);
  }
}
