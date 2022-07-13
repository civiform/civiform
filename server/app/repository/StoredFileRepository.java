package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFile;

/**
 * StoredFileRepository performs complicated operations on {@link StoredFile} that involve
 * asynchronous handling.
 */
public class StoredFileRepository {

  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public StoredFileRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  /** Return all files in a set. */
  public CompletionStage<Set<StoredFile>> list() {
    return supplyAsync(() -> database.find(StoredFile.class).findSet(), executionContext);
  }

  public CompletionStage<List<StoredFile>> lookupFiles(ImmutableList<String> keyNames) {
    return supplyAsync(
        () -> database.find(StoredFile.class).where().in("name", keyNames).findList(),
        executionContext);
  }

  public CompletionStage<Optional<StoredFile>> lookupFile(String keyName) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                database.find(StoredFile.class).where().eq("name", keyName).findOne()),
        executionContext);
  }

  public CompletionStage<Optional<StoredFile>> lookupFile(Long id) {
    return supplyAsync(
        () -> Optional.ofNullable(database.find(StoredFile.class).setId(id).findOne()),
        executionContext);
  }

  public CompletionStage<Void> update(StoredFile storedFile) {
    return supplyAsync(
        () -> {
          database.update(storedFile);
          return null;
        },
        executionContext);
  }

  public CompletionStage<StoredFile> insert(StoredFile file) {
    return supplyAsync(
        () -> {
          database.insert(file);
          return file;
        },
        executionContext);
  }
}
