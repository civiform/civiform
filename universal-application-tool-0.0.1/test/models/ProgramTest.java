package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;

public class ProgramTest extends WithPostgresContainer {

  @Test
  public void persistsChanges() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();

    program.name = "hi";
    program.blocks = BlockContainer.create(ImmutableList.of("hello", "world"));

    program.save();

    Program found = repo.lookupProgram("hi").toCompletableFuture().join().get();

    assertThat(found.blocks.blockDefinitions()).contains("hello", "world");
  }
}
