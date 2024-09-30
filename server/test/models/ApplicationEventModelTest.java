package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationEventRepository;
import repository.ResetPostgres;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;

public class ApplicationEventModelTest extends ResetPostgres {
  private ApplicationEventRepository applicationEventRepository;

  @Before
  public void setUp() {
    applicationEventRepository = instanceOf(ApplicationEventRepository.class);
  }

  @Test
  public void createNonStatusEventDoesNothing() {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    AccountModel adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    ApplicationEventModel event =
        new ApplicationEventModel(
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
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    AccountModel adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    applicationEventRepository.insertStatusEvent(
        application,
        Optional.of(adminAccount),
        StatusEvent.builder().setStatusText("approved").setEmailSent(false).build());

    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of("approved"));

    // Create another event transitioning to empty and ensure that the result is empty.
    applicationEventRepository.insertStatusEvent(
        application,
        Optional.of(adminAccount),
        StatusEvent.builder().setStatusText("").setEmailSent(false).build());
    application.refresh();
    assertThat(application.getLatestStatus()).isEmpty();
  }

  @Test
  public void createStatusEventWithEmptyAccountUpdatesApplicationLatestStatus() {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    applicationEventRepository.insertStatusEvent(
        application,
        Optional.empty(),
        StatusEvent.builder().setStatusText("approved").setEmailSent(false).build());

    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of("approved"));
  }
}
