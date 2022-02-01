package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFile;
import play.db.ebean.EbeanConfig;

/**
 * StoredFileRepository performs complicated operations on {@link StoredFile} that involve
 * asynchronous handling.
 */
public class StoredFileRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public StoredFileRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  /** Return all files in a set. */
  public CompletionStage<Set<StoredFile>> list() {
    return supplyAsync(() -> ebeanServer.find(StoredFile.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<StoredFile>> lookupFile(Long id) {
    return supplyAsync(
        () -> {
          StoredFile file = ebeanServer.find(StoredFile.class).setId(id).findOne();
          if (file == null) {
            return Optional.empty();
          }
          return Optional.of(file);
        },
        executionContext);
  }

  public CompletionStage<Long> insert(StoredFile file) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(file);
          return file.id;
        },
        executionContext);
  }
}
