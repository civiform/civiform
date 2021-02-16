package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import models.Program;
import org.junit.Test;
import services.program.ProgramDefinition;

public class ProgramRepositoryTest extends WithPostgresContainer {

  @Test
  public void createProgram() {
    final ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(10L)
            .setName("name")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of())
            .build();
    Program program = new Program(definition);

    repo.insertProgram(program).toCompletableFuture().join();

    Program found = repo.lookupProgram(10L).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().name()).isEqualTo("name");
  }
}
