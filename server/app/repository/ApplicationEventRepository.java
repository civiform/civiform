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
public class ApplicationEventRepository {
  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApplicationEventRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  /** Insert a new {@link ApplicationEvent} record asynchronously. */
  public CompletionStage<ApplicationEvent> insert(ApplicationEvent event) {
    return supplyAsync(
        () -> {
          database.insert(event);
          return event;
        },
        executionContext);
  }

  /**
   * Returns all {@link ApplicationEvent} records for the {@link Application} with id {@code
   * applicationId} asynchronously.
   */
  public CompletionStage<ImmutableList<ApplicationEvent>> getEvents(Long applicationId) {
    return supplyAsync(
        () -> {
          return ImmutableList.copyOf(
              database
                  .find(ApplicationEvent.class)
                  .where()
                  .eq("application_id", applicationId)
                  .findList());
        },
        executionContext);
  }
}
