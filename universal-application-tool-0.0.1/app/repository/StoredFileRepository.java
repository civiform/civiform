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
  private final AmazonS3Client s3Client;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public StoredFileRepository(
      EbeanConfig ebeanConfig, AmazonS3Client s3Client, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.s3Client = s3Client;
    this.executionContext = executionContext;
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
          file.setContent(s3Client.getObject(file.getName()));
          return Optional.of(file);
        },
        executionContext);
  }

  public CompletionStage<Long> insert(StoredFile file) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(file);
          s3Client.putObject(file.getName(), file.getContent());
          return file.id;
        },
        executionContext);
  }
}
