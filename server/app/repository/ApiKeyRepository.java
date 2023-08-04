package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.PagedList;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApiKey;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;

/**
 * Provides an asynchronous API for persistence and query of {@link ApiKey} instances. Uses {@code
 * DatabaseExecutionContext} for scheduling code to be executed using the database interaction
 * thread pool.
 */
public final class ApiKeyRepository {
  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApiKeyRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  /**
   * List active, i.e. unexpired and unretired, {@link ApiKey}s ordered by creation time descending.
   */
  public PaginationResult<ApiKey> listActiveApiKeys(PageNumberBasedPaginationSpec paginationSpec) {
    Instant now = Instant.now();
    PagedList<ApiKey> pagedList =
        database
            .find(ApiKey.class)
            .where()
            .isNull("retired_time")
            .and()
            .gt("EXTRACT(EPOCH FROM expiration) * 1000", now.toEpochMilli())
            // This is a proxy for creation time descending. Both get the desired ordering
            // behavior but ID is indexed.
            .order("id desc")
            .setFirstRow((paginationSpec.getCurrentPage() - 1) * paginationSpec.getPageSize())
            .setMaxRows(paginationSpec.getPageSize())
            .findPagedList();

    pagedList.loadCount();

    return new PaginationResult<>(
        pagedList.hasNext(),
        pagedList.getTotalPageCount(),
        ImmutableList.copyOf(pagedList.getList()));
  }

  /** List retired {@link ApiKey}s ordered by creation time descending. */
  public PaginationResult<ApiKey> listRetiredApiKeys(PageNumberBasedPaginationSpec paginationSpec) {
    PagedList<ApiKey> pagedList =
        database
            .find(ApiKey.class)
            .where()
            .isNotNull("retired_time")
            // This is a proxy for creation time descending. Both get the desired ordering
            // behavior but ID is indexed.
            .order("id desc")
            .setFirstRow((paginationSpec.getCurrentPage() - 1) * paginationSpec.getPageSize())
            .setMaxRows(paginationSpec.getPageSize())
            .findPagedList();

    pagedList.loadCount();

    return new PaginationResult<>(
        pagedList.hasNext(),
        pagedList.getTotalPageCount(),
        ImmutableList.copyOf(pagedList.getList()));
  }

  /**
   * List expired {@link ApiKey}s ordered by creation time descending. Note that if a key is both
   * retired and expired, it will not be returned here.
   */
  public PaginationResult<ApiKey> listExpiredApiKeys(PageNumberBasedPaginationSpec paginationSpec) {
    Instant now = Instant.now();
    PagedList<ApiKey> pagedList =
        database
            .find(ApiKey.class)
            .where()
            .lt("EXTRACT(EPOCH FROM expiration) * 1000", now.toEpochMilli())
            .and()
            .isNull("retired_time")
            // This is a proxy for creation time descending. Both get the desired ordering
            // behavior but ID is indexed.
            .order("id desc")
            .setFirstRow((paginationSpec.getCurrentPage() - 1) * paginationSpec.getPageSize())
            .setMaxRows(paginationSpec.getPageSize())
            .findPagedList();

    pagedList.loadCount();

    return new PaginationResult<>(
        pagedList.hasNext(),
        pagedList.getTotalPageCount(),
        ImmutableList.copyOf(pagedList.getList()));
  }

  /** Increment an API key's call count and set its last call IP address to the one provided. */
  public void recordApiKeyUsage(String apiKeyId, String remoteAddress) {
    ApiKey apiKey = database.find(ApiKey.class).where().eq("key_id", apiKeyId).findOne();

    apiKey.incrementCallCount();
    apiKey.setLastCallIpAddress(remoteAddress);

    apiKey.save();
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
