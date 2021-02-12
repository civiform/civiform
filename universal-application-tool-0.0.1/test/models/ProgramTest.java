package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import org.junit.Before;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.program.BlockDefinition;

public class ProgramTest extends WithPostgresContainer {

  //  @Test
  //  public void hasProgramDefinitionWhenCreated() {
  //    Program program = new Program();
  ////    assertThat(program.getProgramDefinition()).isInstanceOf(ProgramDefinition.class);
  //  }

  @Test
  public void canSaveProgram() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();
    program.name = "hello";
    program.description = "desc";

    BlockDefinition block =
        BlockDefinition.builder()
            .setId(10L)
            .setName("name")
            .setDescription("it's a block!")
            .build();
    program.blockContainer = new BlockContainer(ImmutableList.of(block));

    program.save();
    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.name).isEqualTo("hello");
    assertThat(found.blockContainer).isInstanceOf(BlockContainer.class);
  }
}
