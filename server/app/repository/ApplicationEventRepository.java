package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import models.ApplicationEvent;

/**
 * ApplicationEventRepository performs operations on {@link ApplicationEvent} that often involve
 * other EBean models or asynchronous handling.
 */
public final class ApplicationEventRepository {
  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApplicationEventRepository(DatabaseExecutionContext executionContext) {
    this.database = checkNotNull(DB.getDefault());
    this.executionContext = checkNotNull(executionContext);
  }

  /** Insert a new {@link ApplicationEvent} record synchronously. */
  public ApplicationEvent insertSync(ApplicationEvent event) {
    database.insert(event);
    event.refresh();
    return event;
  }

  /** Insert a new {@link ApplicationEvent} record asynchronously. */
  public CompletionStage<ApplicationEvent> insertAsync(ApplicationEvent event) {
    return supplyAsync(
        () -> {
          database.insert(event);
          event.refresh();
          return event;
        },
        executionContext.current());
  }

  /**
   * Returns all {@link ApplicationEvent} records for the {@link Application} with id {@code
   * applicationId} synchronously.
   */
  public ImmutableList<ApplicationEvent> getEventsOrderByCreateTimeDesc(Long applicationId) {
    return ImmutableList.copyOf(
        database
            .find(ApplicationEvent.class)
            .where()
            .eq("application_id", applicationId)
            .orderBy()
            .desc("create_time")
            .findList());
  }
}
