package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicationEventModel;
import models.ApplicationModel;

/**
 * ApplicationEventRepository performs operations on {@link ApplicationEventModel} that often
 * involve other EBean models or asynchronous handling.
 */
public final class ApplicationEventRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("ApplicationEventRepository");
  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApplicationEventRepository(DatabaseExecutionContext executionContext) {
    this.database = checkNotNull(DB.getDefault());
    this.executionContext = checkNotNull(executionContext);
  }

  /** Insert a new {@link ApplicationEventModel} record synchronously. */
  public ApplicationEventModel insertSync(ApplicationEventModel event) {
    database.insert(event);
    event.refresh();
    return event;
  }

  /** Insert a new {@link ApplicationEventModel} record asynchronously. */
  public CompletionStage<ApplicationEventModel> insertAsync(ApplicationEventModel event) {
    return supplyAsync(
        () -> {
          database.insert(event);
          event.refresh();
          return event;
        },
        executionContext.current());
  }

  /**
   * Returns all {@link ApplicationEventModel} records for the {@link ApplicationModel} with id
   * {@code applicationId} synchronously.
   */
  public ImmutableList<ApplicationEventModel> getEventsOrderByCreateTimeDesc(Long applicationId) {
    return ImmutableList.copyOf(
        database
            .find(ApplicationEventModel.class)
            .where()
            .eq("application_id", applicationId)
            .orderBy()
            .desc("create_time")
            .setLabel("ApplicationEventModel.findSet")
            .setProfileLocation(
                queryProfileLocationBuilder.create("getEventsOrderByCreateTimeDesc"))
            .findList());
  }
}
