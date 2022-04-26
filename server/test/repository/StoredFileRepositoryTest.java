package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.StoredFile;
import org.junit.Test;

public class StoredFileRepositoryTest extends ResetPostgres {

  @Test
  public void createFileRecord() {
    StoredFile file = new StoredFile();
    file.setName("file name");

    StoredFileRepository repo = instanceOf(StoredFileRepository.class);
    repo.insert(file).toCompletableFuture().join();

    long id = file.id;
    StoredFile f = repo.lookupFile(id).toCompletableFuture().join().get();
    assertThat(f.id).isEqualTo(id);
    assertThat(f.getName()).isEqualTo("file name");
  }
}
