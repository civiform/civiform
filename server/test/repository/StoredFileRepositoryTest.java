package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import models.StoredFile;
import org.junit.Before;
import org.junit.Test;
import support.ProgramBuilder;

public class StoredFileRepositoryTest extends ResetPostgres {

  private StoredFileRepository repo;
  private StoredFile file;

  @Before
  public void setUp() {
    repo = instanceOf(StoredFileRepository.class);
    file = new StoredFile().setName("file name");
  }

  @Test
  public void insert() {
    repo.insert(file).toCompletableFuture().join();

    long id = file.id;
    file = repo.lookupFile(id).toCompletableFuture().join().get();
    assertThat(file.id).isEqualTo(id);
    assertThat(file.getName()).isEqualTo("file name");
  }

  @Test
  public void update_aclChangesArePersisted() {
    file.save();
    file.getAcls()
        .addProgramToReaders(ProgramBuilder.newDraftProgram("program-one").buildDefinition());

    repo.update(file).toCompletableFuture().join();

    file = repo.lookupFile(file.id).toCompletableFuture().join().get();
    assertThat(file.getAcls().getProgramReadAcls()).containsOnly("program-one");
  }

  @Test
  public void lookupFiles() {
    file.save();
    var fileTwo = new StoredFile().setName("file-two");
    fileTwo.save();

    List<StoredFile> result =
        repo.lookupFiles(ImmutableList.of(file.getName(), fileTwo.getName()))
            .toCompletableFuture()
            .join();

    assertThat(result).containsOnly(file, fileTwo);
  }

  @Test
  public void lookupFile() {
    file.save();

    StoredFile result = repo.lookupFile(file.getName()).toCompletableFuture().join().get();

    assertThat(result).isEqualTo(file);
  }
}
