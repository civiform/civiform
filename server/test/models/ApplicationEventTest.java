package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import repository.ResetPostgres;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;

public class ApplicationEventTest extends ResetPostgres {
  @Test
  public void createNonStatusEventDoesNothing() {
    Program program = resourceCreator.insertActiveProgram("test program");

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application application =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    ApplicationEvent event =
        new ApplicationEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.NOTE_CHANGE)
                .setNoteEvent(NoteEvent.create("some note"))
                .build());
    event.save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEmpty();
  }

  @Test
  public void createStatusEventUpdatesApplicationLatestStatus() {
    Program program = resourceCreator.insertActiveProgram("test program");

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application application =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    new ApplicationEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(
                    StatusEvent.builder().setStatusText("approved").setEmailSent(false).build())
                .build())
        .save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of("approved"));

    // Create another event transitioning to empty and ensure that the result is empty.
    new ApplicationEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(StatusEvent.builder().setStatusText("").setEmailSent(false).build())
                .build())
        .save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEmpty();
  }

  @Test
  public void eventTriggerUsesLatestStatusEvent() throws Exception {
    Program program = resourceCreator.insertActiveProgram("test program");

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application application =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    ApplicationEvent firstEvent =
        new ApplicationEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(
                    StatusEvent.builder().setStatusText("approved").setEmailSent(false).build())
                .build());
    firstEvent.save();

    // When persisting models with @WhenModified fields, EBean
    // truncates the persisted timestamp to milliseconds:
    // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
    // Sleep for a few milliseconds to ensure that a subsequent
    // update would have a distinct timestamp.
    TimeUnit.MILLISECONDS.sleep(5);

    ApplicationEvent secondEvent =
        new ApplicationEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(
                    StatusEvent.builder().setStatusText("rejected").setEmailSent(false).build())
                .build());
    secondEvent.save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of("rejected"));

    // Update the first event to have a new creator and ensure that we still use the most recently
    // created event to update latest_status.
    firstEvent.setCreator(resourceCreator.insertAccountWithEmail("someotheraccount@example.com"));
    firstEvent.save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of("rejected"));
  }
}
