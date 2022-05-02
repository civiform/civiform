package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApiKey;

/**
 * Provides an asynchronous API for persistence and query of {@link ApiKey} instances. Uses {@code
 * DatabaseExecutionContext} for scheduling code to be executed using the database interaction
 * thread pool.
 */
public class ApiKeyRepository {
  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApiKeyRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  /** Insert a new {@link ApiKey} record asynchronously. */
  public CompletionStage<ApiKey> insert(ApiKey apiKey) {
    return supplyAsync(
        () -> {
          database.insert(apiKey);
          return apiKey;
        },
        executionContext);
  }

  /** Find an ApiKey record by database primary ID asynchronously. */
  public CompletionStage<Optional<ApiKey>> lookupApiKey(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(database.find(ApiKey.class).setId(id).findOne()),
        executionContext);
  }

  /** Find an ApiKey record by the key's string ID asynchronously. */
  public CompletionStage<Optional<ApiKey>> lookupApiKey(String keyId) {
    return supplyAsync(
        () ->
            Optional.ofNullable(database.find(ApiKey.class).where().eq("key_id", keyId).findOne()),
        executionContext);
  }
}
