package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApiBridgeConfigurationModel;
import services.apibridge.ApiBridgeServiceDto.CompatibilityLevel;

/**
 * Provides an asynchronous API for persistence and query of {@link
 * ApiBridgeConfigurationRepository} instances. Uses {@link DatabaseExecutionContext} for scheduling
 * code to be executed using the database interaction thread pool.
 */
public final class ApiBridgeConfigurationRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("ApiBridgeRepository");

  private final Database database;
  private final DatabaseExecutionContext dbExecutionContext;

  @Inject
  public ApiBridgeConfigurationRepository(DatabaseExecutionContext dbExecutionContext) {
    this.database = DB.getDefault();
    this.dbExecutionContext = checkNotNull(dbExecutionContext);
  }

  /** Returns all {@link ApiBridgeConfigurationModel} records asynchronously. */
  public CompletionStage<ImmutableList<ApiBridgeConfigurationModel>> findAll() {
    return supplyAsync(
        () ->
            database
                .find(ApiBridgeConfigurationModel.class)
                .setLabel("ApiBridgeConfigurationRepository.findAll")
                .setProfileLocation(queryProfileLocationBuilder.create("findAll"))
                .findList()
                .stream()
                .collect(ImmutableList.toImmutableList()),
        dbExecutionContext);
  }

  /** Returns an {@link ApiBridgeConfigurationModel} record or empty optional asynchronously. */
  public CompletionStage<Optional<ApiBridgeConfigurationModel>>
      findByHostUrlAndUrlPathAndCompatibilityLevel(
          String hostUrl, String urlPath, CompatibilityLevel compatibilityLevel) {
    return supplyAsync(
        () ->
            database
                .find(ApiBridgeConfigurationModel.class)
                .where()
                .eq("host_url", hostUrl)
                .and()
                .eq("url_path", urlPath)
                .and()
                .eq("compatibilityLevel", compatibilityLevel)
                .setLabel(
                    "ApiBridgeConfigurationRepository.findByHostUrlAndUrlPathAndCompatibilityLevel")
                .setProfileLocation(
                    queryProfileLocationBuilder.create(
                        "findByHostUrlAndUrlPathAndCompatibilityLevel"))
                .findOneOrEmpty(),
        dbExecutionContext);
  }

  /** Insert a new {@link ApiBridgeConfigurationModel} record asynchronously. */
  public CompletionStage<ApiBridgeConfigurationModel> insert(ApiBridgeConfigurationModel model) {
    return supplyAsync(
        () -> {
          database.insert(model);
          return model;
        },
        dbExecutionContext);
  }

  /** Update a new {@link ApiBridgeConfigurationModel} record asynchronously. */
  public CompletionStage<ApiBridgeConfigurationModel> update(ApiBridgeConfigurationModel model) {
    return supplyAsync(
        () -> {
          database.update(model);
          return model;
        },
        dbExecutionContext);
  }

  /** Delete a new {@link ApiBridgeConfigurationModel} record asynchronously. */
  public CompletionStage<Boolean> delete(Long id) {
    return supplyAsync(
        () -> database.delete(ApiBridgeConfigurationModel.class, id) == 1, dbExecutionContext);
  }
}
