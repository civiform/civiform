package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Query;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApiKeyModel;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.PaginationResult;

/**
 * Provides an asynchronous API for persistence and query of {@link ApiKeyModel} instances. Uses
 * {@code DatabaseExecutionContext} for scheduling code to be executed using the database
 * interaction thread pool.
 */
public final class ApiKeyRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("ApiKeyRepository");
  private final Database database;
  private final TransactionManager transactionManager;
  private final DatabaseExecutionContext dbExecutionContext;

  @Inject
  public ApiKeyRepository(DatabaseExecutionContext dbExecutionContext) {
    this.database = DB.getDefault();
    this.transactionManager = new TransactionManager();
    this.dbExecutionContext = checkNotNull(dbExecutionContext);
  }

  /**
   * List active, i.e. unexpired and unretired, {@link ApiKeyModel}s ordered by creation time
   * descending.
   */
  public PaginationResult<ApiKeyModel> listActiveApiKeys(PageNumberPaginationSpec paginationSpec) {
    Instant now = Instant.now();
    Query<ApiKeyModel> query =
        database
            .find(ApiKeyModel.class)
            .where()
            .isNull("retired_time")
            .and()
            .gt("EXTRACT(EPOCH FROM expiration) * 1000", now.toEpochMilli())
            .setLabel("ApiKeyModel.findList")
            .setProfileLocation(queryProfileLocationBuilder.create("listActiveApiKeys"));
    PagedList<ApiKeyModel> pagedList = paginationSpec.apply(query).findPagedList();

    pagedList.loadCount();

    return new PaginationResult<>(
        pagedList.hasNext(),
        pagedList.getTotalPageCount(),
        ImmutableList.copyOf(pagedList.getList()));
  }

  /** List retired {@link ApiKeyModel}s ordered by creation time descending. */
  public PaginationResult<ApiKeyModel> listRetiredApiKeys(PageNumberPaginationSpec paginationSpec) {
    Query<ApiKeyModel> query =
        database
            .find(ApiKeyModel.class)
            .where()
            .isNotNull("retired_time")
            .setLabel("ApiKeyModel.findList")
            .setProfileLocation(queryProfileLocationBuilder.create("listRetiredApiKeys"));
    PagedList<ApiKeyModel> pagedList = paginationSpec.apply(query).findPagedList();

    pagedList.loadCount();

    return new PaginationResult<>(
        pagedList.hasNext(),
        pagedList.getTotalPageCount(),
        ImmutableList.copyOf(pagedList.getList()));
  }

  /**
   * List expired {@link ApiKeyModel}s ordered by creation time descending. Note that if a key is
   * both retired and expired, it will not be returned here.
   */
  public PaginationResult<ApiKeyModel> listExpiredApiKeys(PageNumberPaginationSpec paginationSpec) {
    Instant now = Instant.now();
    Query<ApiKeyModel> query =
        database
            .find(ApiKeyModel.class)
            .where()
            .lt("EXTRACT(EPOCH FROM expiration) * 1000", now.toEpochMilli())
            .and()
            .isNull("retired_time")
            .setLabel("ApiKeyModel.findList")
            .setProfileLocation(queryProfileLocationBuilder.create("listExpiredApiKeys"));
    PagedList<ApiKeyModel> pagedList = paginationSpec.apply(query).findPagedList();

    pagedList.loadCount();

    return new PaginationResult<>(
        pagedList.hasNext(),
        pagedList.getTotalPageCount(),
        ImmutableList.copyOf(pagedList.getList()));
  }

  /** Increment an API key's call count and set its last call IP address to the one provided. */
  public void recordApiKeyUsage(String apiKeyId, String remoteAddress) {
    transactionManager.executeWithRetry(
        () -> {
          Optional<ApiKeyModel> apiKey =
              database
                  .find(ApiKeyModel.class)
                  .where()
                  .eq("key_id", apiKeyId)
                  .setLabel("ApiKeyModel.findById")
                  .setProfileLocation(queryProfileLocationBuilder.create("recordApiKeyUsage"))
                  .findOneOrEmpty();

          if (apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key %s not found".formatted(apiKeyId));
          }

          apiKey.get().incrementCallCount().setLastCallIpAddress(remoteAddress).save();
        });
  }

  /** Insert a new {@link ApiKeyModel} record asynchronously. */
  public CompletionStage<ApiKeyModel> insert(ApiKeyModel apiKey) {
    return supplyAsync(
        () -> {
          database.insert(apiKey);
          return apiKey;
        },
        dbExecutionContext);
  }

  /** Find an ApiKey record by database primary ID asynchronously. */
  public CompletionStage<Optional<ApiKeyModel>> lookupApiKey(long id) {
    return supplyAsync(
        () ->
            database
                .find(ApiKeyModel.class)
                .setId(id)
                .setLabel("ApiKeyModel.findById")
                .setProfileLocation(queryProfileLocationBuilder.create("lookupApiKey"))
                .findOneOrEmpty(),
        dbExecutionContext);
  }

  /** Find an ApiKey record by the key's string ID asynchronously. */
  public CompletionStage<Optional<ApiKeyModel>> lookupApiKey(String keyId) {
    return supplyAsync(
        () ->
            database
                .find(ApiKeyModel.class)
                .where()
                .eq("key_id", keyId)
                .setLabel("ApiKeyModel.findById")
                .setProfileLocation(queryProfileLocationBuilder.create("lookupApiKey"))
                .findOneOrEmpty(),
        dbExecutionContext);
  }
}
