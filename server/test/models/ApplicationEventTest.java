package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;
import services.program.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationEventTest extends ResetPostgres {

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .build();

  @Test
  public void createNonStatusEventDoesNothing() {
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application application =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    ApplicationEvent event =
        new ApplicationEvent(
            application,
            adminAccount,
            ApplicationEventDetails.Type.NOTE_CHANGE,
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
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application application =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    new ApplicationEvent(
            application,
            adminAccount,
            ApplicationEventDetails.Type.STATUS_CHANGE,
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(
                    StatusEvent.builder()
                        .setStatusText(APPROVED_STATUS.statusText())
                        .setEmailSent(false)
                        .build())
                .build())
        .save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of(APPROVED_STATUS.statusText()));

    // Create another event transitioning to empty and ensure that the result is empty.
    new ApplicationEvent(
            application,
            adminAccount,
            ApplicationEventDetails.Type.STATUS_CHANGE,
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(StatusEvent.builder().setStatusText("").setEmailSent(false).build())
                .build())
        .save();

    application.refresh();
    assertThat(application.getLatestStatus()).isEmpty();
  }
}
