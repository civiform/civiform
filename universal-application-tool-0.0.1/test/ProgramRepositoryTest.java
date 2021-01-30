import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import models.Program;
import org.junit.Test;
import play.Application;
import play.test.WithApplication;
import repository.ProgramRepository;

public class ProgramRepositoryTest extends WithApplication {
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
            "jdbc:tc:postgresql:9.6.8:///databasename"));
  }

  @Test
  public void createProgram() {
    // arrange
    final ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();
    program.name = "program one";
    program.version = 1L;
    program.object = ImmutableMap.of("key", "value");
    // act
    repo.insertProgram(program).toCompletableFuture().join();
    // assert
    Program p = repo.lookupProgram("program one", 1L).toCompletableFuture().join().get();
    assertThat(p.version).isEqualTo(1L);
    assertThat(p.object).containsEntry("key", "value");
  }
}
