package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.SettingsGroup;

/** Contains queries related to the server settings system. */
public final class SettingsGroupRepository {

  private final Database database;
  private final DatabaseExecutionContext databaseExecutionContext;

  @Inject
  public SettingsGroupRepository(DatabaseExecutionContext databaseExecutionContext) {
    this.database = DB.getDefault();
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
  }

  /** Get the most recently created {@link SettingsGroup}. */
  public CompletionStage<Optional<SettingsGroup>> getCurrentSettings() {
    return supplyAsync(
        () ->
            database
                .find(SettingsGroup.class)
                .orderBy()
                .desc("create_time")
                .setMaxRows(1)
                .findOneOrEmpty(),
        databaseExecutionContext);
  }
}
