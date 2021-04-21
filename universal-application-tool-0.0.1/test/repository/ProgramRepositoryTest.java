package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import services.program.TranslationNotFoundException;

public class ProgramRepositoryTest extends WithPostgresContainer {

  private ProgramRepository repo;

  @Before
  public void setupProgramRepository() {
    repo = instanceOf(ProgramRepository.class);
  }

  @Test
  public void listPrograms_empty() {
    List<Program> allPrograms = repo.listPrograms().toCompletableFuture().join();

    assertThat(allPrograms).isEmpty();
  }

  @Test
  public void listPrograms() {
    Program one = resourceCreator.insertProgram("one");
    Program two = resourceCreator.insertProgram("two");

    ImmutableList<Program> allPrograms = repo.listPrograms().toCompletableFuture().join();

    assertThat(allPrograms).containsExactly(one, two);
  }

  @Test
  public void lookupProgram_returnsEmptyOptionalWhenProgramNotFound() {
    Optional<Program> found = repo.lookupProgram(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupProgram_findsCorrectProgram() {
    resourceCreator.insertProgram("one");
    Program two = resourceCreator.insertProgram("two");

    Optional<Program> found = repo.lookupProgram(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void insertProgramSync() throws TranslationNotFoundException {
    Program program = new Program("ProgramRepository", "desc", "name", "description");

    Program withId = repo.insertProgramSync(program);

    Program found = repo.lookupProgram(withId.id).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().getLocalizedName(Locale.US)).isEqualTo("name");
  }

  @Test
  public void updateProgramSync() {
    Program existing = resourceCreator.insertProgram("old name");
    Program updates =
        new Program(
            existing.getProgramDefinition().toBuilder()
                .setLocalizedName(ImmutableMap.of(Locale.US, "new name"))
                .build());

    Program updated = repo.updateProgramSync(updates);

    assertThat(updated.getProgramDefinition().id()).isEqualTo(existing.id);
    assertThat(updated.getProgramDefinition().localizedName())
        .isEqualTo(ImmutableMap.of(Locale.US, "new name"));
  }
}
