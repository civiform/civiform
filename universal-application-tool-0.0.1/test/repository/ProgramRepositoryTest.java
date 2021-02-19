package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.Program;
import org.junit.Test;

public class ProgramRepositoryTest extends WithPostgresContainer {

  @Test
  public void createProgram() {
    final ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);

    Program program = new Program("ProgramRepository", "desc");

    Program withId = repo.insertProgramSync(program);

    Program found = repo.lookupProgram(withId.id).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().name()).isEqualTo("ProgramRepository");
  }
}
