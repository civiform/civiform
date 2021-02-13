package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.StoredFile;
import org.junit.Test;

public class StoredFileRepositoryTest extends WithPostgresContainer {

  @Test
  public void createFile() {
    final StoredFileRepository repo = app.injector().instanceOf(StoredFileRepository.class);
    StoredFile file = new StoredFile();
    file.setName("file name");

    repo.insert(file).toCompletableFuture().join();

    long id = file.id;
    StoredFile f = repo.lookupFile(id).toCompletableFuture().join().get();
    assertThat(f.id).isEqualTo(id);
    assertThat(f.getName()).isEqualTo("file name");
  }
}
