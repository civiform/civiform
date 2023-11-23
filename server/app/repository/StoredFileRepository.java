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
import models.StoredFileModel;

/**
 * StoredFileRepository performs complicated operations on {@link StoredFileModel} that involve
 * asynchronous handling.
 */
public final class StoredFileRepository {
  private final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("StoredFileRepository");

  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public StoredFileRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  /** Return all files in a set. */
  public CompletionStage<Set<StoredFileModel>> list() {
    return supplyAsync(
        () ->
            database
                .find(StoredFileModel.class)
                .setLabel("StoredFile.findSet")
                .setProfileLocation(queryProfileLocationBuilder.create("list"))
                .findSet(),
        executionContext);
  }

  public CompletionStage<List<StoredFileModel>> lookupFiles(ImmutableList<String> keyNames) {
    return supplyAsync(
        () ->
            database
                .find(StoredFileModel.class)
                .setLabel("StoredFile.findList")
                .setProfileLocation(queryProfileLocationBuilder.create("lookupFiles"))
                .where()
                .in("name", keyNames)
                .findList(),
        executionContext);
  }

  public CompletionStage<Optional<StoredFileModel>> lookupFile(String keyName) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                database
                    .find(StoredFileModel.class)
                    .setLabel("StoredFile.findByName")
                    .setProfileLocation(queryProfileLocationBuilder.create("lookupFile"))
                    .where()
                    .eq("name", keyName)
                    .findOne()),
        executionContext);
  }

  public CompletionStage<Optional<StoredFileModel>> lookupFile(Long id) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                database
                    .find(StoredFileModel.class)
                    .setLabel("StoredFile.findOne")
                    .setProfileLocation(queryProfileLocationBuilder.create("lookupFile"))
                    .setId(id)
                    .findOne()),
        executionContext);
  }

  public CompletionStage<Void> update(StoredFileModel storedFile) {
    return supplyAsync(
        () -> {
          database.update(storedFile);
          return null;
        },
        executionContext);
  }

  public CompletionStage<StoredFileModel> insert(StoredFileModel file) {
    return supplyAsync(
        () -> {
          database.insert(file);
          return file;
        },
        executionContext);
  }
}
