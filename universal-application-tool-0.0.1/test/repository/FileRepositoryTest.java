package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.File;
import org.junit.Test;

public class FileRepositoryTest extends WithPostgresContainer {

  @Test
  public void createFile() {
    final FileRepository repo = app.injector().instanceOf(FileRepository.class);
    File file = new File();
    file.setName("file name");

    repo.insert(file).toCompletableFuture().join();

    long id = file.id;
    File f = repo.lookupFile(id).toCompletableFuture().join().get();
    assertThat(f.id).isEqualTo(id);
    assertThat(f.getName()).isEqualTo("file name");
  }
}
