package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.AccountModel;
import models.ApplicationEventModel;
import models.ApplicationModel;
import services.application.ApplicationEventDetails;

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

  /**
   * Insert a new {@link ApplicationEventModel} record synchronously. Inserts to the
   * ApplicationEvents table should be done only by the ApplicationEventRepository as any inserts
   * will also need an update on the Applications table
   */
  @VisibleForTesting
  ApplicationEventModel insertSync(ApplicationEventModel event) {
    database.insert(event);
    event.refresh();
    return event;
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

  /**
   * Updates the ApplicationEvents and the Applications table to the latest status in a single
   * transaction.
   *
   * <p>Previously in 44.sql, DB triggers were used to update the latest_status between
   * application_events table and applications table. In 78.sql script, the team decided to drop
   * triggers and use application code to update both tables. This method implements the same.
   *
   * <p>Note - Application code must change both tables at once as we want to avoid inconsistency
   * between the tables.
   */
  public CompletionStage<ApplicationEventModel> insertStatusEvent(
      ApplicationModel application,
      Optional<AccountModel> optionalAdmin,
      ApplicationEventDetails.StatusEvent newStatusEvent) {
    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(newStatusEvent)
            .build();
    ApplicationEventModel event = new ApplicationEventModel(application, optionalAdmin, details);
    return supplyAsync(
        () -> {
          try (Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
            insertSync(event);
            // Saves the latest note on the applications table too.
            // If the status is removed from an application, then the latest_status column needs
            // to be set to null to indicate the application has no status and not a status with
            // empty string.
            database
                .update(ApplicationModel.class)
                .set(
                    "latest_status",
                    Strings.isNullOrEmpty(newStatusEvent.statusText())
                        ? null
                        : newStatusEvent.statusText())
                .where()
                .eq("id", application.id)
                .update();
            application.save();
            transaction.commit();
          }
          return event;
        },
        executionContext.current());
  }

  public void insertNoteEvent(
      ApplicationModel application, ApplicationEventDetails.NoteEvent note, AccountModel admin) {
    ApplicationEventDetails details =
        services.application.ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.NOTE_CHANGE)
            .setNoteEvent(note)
            .build();
    ApplicationEventModel event =
        new ApplicationEventModel(application, Optional.of(admin), details);
    try (Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
      insertSync(event);
      // save the latest note on the applications table too
      database
          .update(ApplicationModel.class)
          .set("latest_note", note.note())
          .where()
          .eq("id", application.id)
          .update();
      application.save();
      transaction.commit();
    }
  }
}
