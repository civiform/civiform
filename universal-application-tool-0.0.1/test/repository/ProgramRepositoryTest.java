package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import models.Program;
import org.junit.Test;
import services.program.ProgramDefinition;

public class ProgramRepositoryTest extends WithResettingPostgresContainer {

  @Test
  public void createProgram() {
    final ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setName("ProgramRepository")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of())
            .build();
    Program program = new Program(definition);

    repo.insertProgramSync(program);

    Program found = repo.lookupProgram(1L).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().name()).isEqualTo("ProgramRepository");
  }
}
