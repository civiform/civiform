package support;

import static org.assertj.core.api.Assertions.assertThat;

import models.Program;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;

public class ProgramBuilderWithPostgresContainerTest extends WithPostgresContainer {

  @Test
  public void test() {
    Program program = ProgramBuilder.newProgram().build();

    Program found =
        app.injector()
            .instanceOf(ProgramRepository.class)
            .lookupProgram(program.id)
            .toCompletableFuture()
            .join()
            .get();

    assertThat(found).isEqualTo(program);
  }
}
