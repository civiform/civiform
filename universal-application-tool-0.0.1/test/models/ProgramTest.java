package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;

public class ProgramTest extends WithPostgresContainer {

  @Test
  public void saveBasicProgram() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setName("name")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of())
            .build();
    program.name = "name";
    program.description = "description";
    program.id = 1L;

    program.save();

    Program found = repo.lookupProgram("name").toCompletableFuture().join().get();

    assertThat(found.name).isEqualTo("name");
    assertThat(found.description).isEqualTo("description");
  }
}
