package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import models.Program;
import org.junit.Test;

public class ProgramRepositoryTest extends WithPostgresContainer {

  @Test
  public void createProgram() {
    // arrange
    final ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();
    //    program.setName("program one");
    //    program.setVersion(1L);
    //    program.setObject(ImmutableMap.of("key", "value"));
    //    // act
    //    repo.insertProgram(program).toCompletableFuture().join();
    //    // assert
    //    Program p = repo.lookupProgram("program one", 1L).toCompletableFuture().join().get();
    //    assertThat(p.getVersion()).isEqualTo(1L);
    //    assertThat(p.getObject()).containsEntry("key", "value");
  }
}
