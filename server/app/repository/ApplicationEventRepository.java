package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import models.Application;
import models.ApplicationEvent;

/**
 * ApplicationEventRepository performs operations on {@link ApplicationEvent} that often involve
 * other EBean models or asynchronous handling.
 */
public final class ApplicationEventRepository {
  private final Database database;

  public ApplicationEventRepository() {
    this.database = checkNotNull(DB.getDefault());
  }

  /** Insert a new {@link ApplicationEvent} record asynchronously. */
  public ApplicationEvent insertSync(ApplicationEvent event) {
    database.insert(event);
    event.refresh();
    return event;
  }

  /**
   * Returns all {@link ApplicationEvent} records for the {@link Application} with id {@code
   * applicationId} asynchronously.
   */
  public ImmutableList<ApplicationEvent> getEvents(Long applicationId) {
    return ImmutableList.copyOf(
        database
            .find(ApplicationEvent.class)
            .where()
            .eq("application_id", applicationId)
            .findList());
  }
}
