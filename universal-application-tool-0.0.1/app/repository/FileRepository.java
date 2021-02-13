package repository;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.File;
import play.db.ebean.EbeanConfig;

public class FileRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public FileRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.executionContext = executionContext;
  }

  /** Return all files in a set. */
  public CompletionStage<Set<File>> list() {
    return supplyAsync(() -> ebeanServer.find(File.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<File>> lookupFile(Long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(File.class).setId(id).findOne()),
        executionContext);
  }

  public CompletionStage<Long> insert(File file) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(file);
          return file.id;
        },
        executionContext);
  }
}
