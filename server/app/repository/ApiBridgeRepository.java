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

public class ApiBridgeRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("ApiBridgeRepository");

  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApiBridgeRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  public CompletionStage<ImmutableList<ApiBridgeConfigurationModel>> getAll() {
    return supplyAsync(
        () ->
            database
                .find(ApiBridgeConfigurationModel.class)
                .setLabel("ApiBridgeRepository.getAll")
                .findSet()
                .stream()
                .collect(ImmutableList.toImmutableList()),
        executionContext);
  }

  public CompletionStage<ImmutableList<ApiBridgeConfigurationModel>> getAllEnabled() {
    return supplyAsync(
        () ->
            database
                .find(ApiBridgeConfigurationModel.class)
                .where()
                .eq("enabled", true)
                .setLabel("ApiBridgeRepository.getAllEnabled")
                .findSet()
                .stream()
                .collect(ImmutableList.toImmutableList()),
        executionContext);
  }

  public CompletionStage<Optional<ApiBridgeConfigurationModel>> getById(Long id) {
    return supplyAsync(
        () ->
            database
                .find(ApiBridgeConfigurationModel.class)
                .where()
                .eq("id", id)
                .setLabel("ApiBridgeRepository.getBridgeConfigurationById")
                .findOneOrEmpty(),
        executionContext);
  }

  public CompletionStage<Optional<ApiBridgeConfigurationModel>> getBridgeConfigurationById(
      String hostUri, String uriPath, String compatibilityLevel) {
    return supplyAsync(
        () ->
            database
                .find(ApiBridgeConfigurationModel.class)
                .where()
                .eq("host_uri", hostUri)
                .and()
                .eq("uri_path", uriPath)
                .and()
                .eq("compatibilityLevel", compatibilityLevel)
                .setLabel("ApiBridgeRepository.getBridgeConfigurationById")
                .findOneOrEmpty(),
        executionContext);
  }

  /** Insert a new {@link ApiBridgeConfigurationModel} record asynchronously. */
  public CompletionStage<ApiBridgeConfigurationModel> insert(ApiBridgeConfigurationModel model) {
    return supplyAsync(
        () -> {
          database.insert(model);
          return model;
        },
        executionContext);
  }

  /** Update a new {@link ApiBridgeConfigurationModel} record asynchronously. */
  public CompletionStage<ApiBridgeConfigurationModel> update(ApiBridgeConfigurationModel model) {
    return supplyAsync(
        () -> {
          database.update(model);
          return model;
        },
        executionContext);
  }

  /** Delete a new {@link ApiBridgeConfigurationModel} record asynchronously. */
  public CompletionStage<Boolean> delete(Long id) {
    return supplyAsync(
        () -> database.delete(ApiBridgeConfigurationModel.class, id) == 1, executionContext);
  }
}
