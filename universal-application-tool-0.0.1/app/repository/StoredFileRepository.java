package repository;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFile;
import play.db.ebean.EbeanConfig;

public class StoredFileRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public StoredFileRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.executionContext = executionContext;
  }

  /** Return all files in a set. */
  public CompletionStage<Set<StoredFile>> list() {
    return supplyAsync(() -> ebeanServer.find(StoredFile.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<StoredFile>> lookupFile(Long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(StoredFile.class).setId(id).findOne()),
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
