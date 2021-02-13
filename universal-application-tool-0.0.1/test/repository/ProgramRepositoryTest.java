package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import models.BlockContainer;
import models.Program;
import org.junit.Test;

public class ProgramRepositoryTest extends WithPostgresContainer {

  @Test
  public void createProgram() {
    final ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();
    program.name = "name";
    program.blocks = BlockContainer.create(ImmutableList.of("hello"));

    repo.insertProgram(program).toCompletableFuture().join();

    Program p = repo.lookupProgram("name").toCompletableFuture().join().get();
    assertThat(p.blocks.blockDefinitions()).contains("hello");
  }
}
