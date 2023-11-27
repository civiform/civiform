package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.SettingsGroupModel;

/** Contains queries related to the server settings system. */
public final class SettingsGroupRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("SettingsGroupRepository");

  private final Database database;
  private final DatabaseExecutionContext databaseExecutionContext;

  @Inject
  public SettingsGroupRepository(DatabaseExecutionContext databaseExecutionContext) {
    this.database = DB.getDefault();
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
  }

  /** Get the most recently created {@link SettingsGroupModel}. */
  public CompletionStage<Optional<SettingsGroupModel>> getCurrentSettings() {
    return supplyAsync(
        () ->
            database
                .find(SettingsGroupModel.class)
                .orderBy()
                .desc("create_time")
                .setMaxRows(1)
                .setLabel("SettingsGroupModel.findOne")
                .setProfileLocation(queryProfileLocationBuilder.create("getCurrentSettings"))
                .findOneOrEmpty(),
        databaseExecutionContext);
  }
}
