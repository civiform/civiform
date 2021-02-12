package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import com.google.common.collect.ImmutableList;
import repository.ApplicantRepository;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;

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
    program.blockContainer = new BlockContainer(ImmutableList.of());
    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.name).isEqualTo("hello");
  }
}
