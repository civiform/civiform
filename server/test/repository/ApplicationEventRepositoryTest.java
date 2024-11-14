package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationEventModel;
import models.ApplicationModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;

public class ApplicationEventRepositoryTest extends ResetPostgres {

  private ApplicationEventRepository repo;
  Database database;

  @Before
  public void setUp() {
    repo = instanceOf(ApplicationEventRepository.class);
    database = DB.getDefault();
  }

  @Test
  public void insert() {
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();
    ApplicationEventModel event =
        new ApplicationEventModel(application, Optional.of(actor), details);
    ApplicationEventModel insertedEvent = repo.insertSync(event);
    // Generated values.
    assertThat(insertedEvent.id).isNotNull();
    assertThat(insertedEvent.getCreateTime()).isAfter(startInstant);
    // Pass through values.
    assertThat(insertedEvent.getApplication()).isEqualTo(application);
    assertThat(insertedEvent.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(insertedEvent.getDetails()).isEqualTo(details);
    assertThat(insertedEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
  }

  @Test
  public void insertAsync() {
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(false).build())
            .build();
    ApplicationEventModel insertedEvent =
        repo.insertStatusEvent(
                application,
                Optional.empty(),
                StatusEvent.builder().setStatusText("Status").setEmailSent(false).build())
            .toCompletableFuture()
            .join();
    // Generated values.
    assertThat(insertedEvent.id).isNotNull();
    assertThat(insertedEvent.getCreateTime()).isAfter(startInstant);
    // Pass through values.
    assertThat(insertedEvent.getApplication()).isEqualTo(application);
    assertThat(insertedEvent.getCreator()).isEqualTo(Optional.empty());
    assertThat(insertedEvent.getDetails()).isEqualTo(details);
    assertThat(insertedEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
  }

  @Test
  public void insertStatusEvents_succeeds() {
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant1 = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application1 = resourceCreator.insertActiveApplication(applicant1, program);

    ApplicantModel applicant2 = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application2 = resourceCreator.insertActiveApplication(applicant2, program);

    ApplicationEventDetails initialStatus =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();

    repo.insertStatusEvents(
        ImmutableList.of(application1, application2),
        Optional.of(actor),
        initialStatus.statusEvent().get());

    ImmutableList<ApplicationEventModel> insertedEventFor1 =
        repo.getEventsOrderByCreateTimeDesc(application1.id);
    ImmutableList<ApplicationEventModel> insertedEventFor2 =
        repo.getEventsOrderByCreateTimeDesc(application2.id);

    assertThat(insertedEventFor1).isNotEmpty();
    assertThat(insertedEventFor2).isNotEmpty();

    assertThat(insertedEventFor1.size()).isEqualTo(1);
    assertThat(insertedEventFor2.size()).isEqualTo(1);

    var insertedEvent1 = insertedEventFor1.get(0);
    var insertedEvent2 = insertedEventFor2.get(0);

    // Evaluate.
    assertThat(insertedEvent1.id).isNotEqualTo(insertedEvent2.id);
    // Generated values.
    assertThat(insertedEvent2.id).isNotNull();
    assertThat(insertedEvent2.getCreateTime()).isAfter(startInstant);
    // Pass through values.
    assertThat(insertedEvent1.getApplication()).isEqualTo(application1);
    assertThat(insertedEvent1.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(insertedEvent1.getDetails()).isEqualTo(initialStatus);
    assertThat(insertedEvent1.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    assertThat(insertedEvent2.getApplication()).isEqualTo(application2);
    assertThat(insertedEvent2.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(insertedEvent2.getDetails()).isEqualTo(initialStatus);
    assertThat(insertedEvent2.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);

    application2.refresh();
    application1.refresh();
    assertThat(application1.getLatestStatus().get()).isEqualTo("Status");
    assertThat(application2.getLatestStatus().get()).isEqualTo("Status");
  }

  @Test
  public void insertStatusEvents_changeApplicationStatus_succeeds() {
    // For  application1, we will change the status from "Status" to "Denied".
    // For application2, we will change the status from "Status" to no status.
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant1 = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application1 = resourceCreator.insertActiveApplication(applicant1, program);

    ApplicantModel applicant2 = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application2 = resourceCreator.insertActiveApplication(applicant2, program);

    ApplicationEventDetails initialStatus =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();
    ApplicationEventDetails statusChange =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Denied").setEmailSent(true).build())
            .build();
    ApplicationEventDetails statusRemoved =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(StatusEvent.builder().setStatusText("").setEmailSent(true).build())
            .build();

    // insert initial status
    repo.insertStatusEvents(
        ImmutableList.of(application1, application2),
        Optional.of(actor),
        initialStatus.statusEvent().get());

    repo.insertStatusEvents(
        ImmutableList.of(application1), Optional.of(actor), statusChange.statusEvent().get());
    repo.insertStatusEvents(
        ImmutableList.of(application2), Optional.of(actor), statusRemoved.statusEvent().get());

    ImmutableList<ApplicationEventModel> insertedEventFor1 =
        repo.getEventsOrderByCreateTimeDesc(application1.id);
    ImmutableList<ApplicationEventModel> insertedEventFor2 =
        repo.getEventsOrderByCreateTimeDesc(application2.id);

    assertThat(insertedEventFor1).isNotEmpty();
    assertThat(insertedEventFor2).isNotEmpty();

    assertThat(insertedEventFor1.size()).isEqualTo(2);
    assertThat(insertedEventFor2.size()).isEqualTo(2);

    var insertedEvent1 = insertedEventFor1.get(1);
    var insertedEvent2 = insertedEventFor2.get(1);

    // Evaluate.
    assertThat(insertedEvent1.id).isNotEqualTo(insertedEvent2.id);

    assertThat(insertedEvent1.getApplication()).isEqualTo(application1);
    assertThat(insertedEvent1.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(insertedEvent1.getDetails()).isEqualTo(initialStatus);
    assertThat(insertedEvent1.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    assertThat(insertedEvent2.getApplication()).isEqualTo(application2);
    assertThat(insertedEvent2.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(insertedEvent2.getDetails()).isEqualTo(initialStatus);
    assertThat(insertedEvent2.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);

    application2.refresh();
    application1.refresh();
    assertThat(application1.getLatestStatus().get()).isEqualTo("Denied");
    assertThat(application2.getLatestStatus()).isEmpty();
  }

  @Test
  public void insertMultipleEventsOnApplication() {
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();

    ApplicationEventModel event1 =
        new ApplicationEventModel(application, Optional.of(actor), details);
    ApplicationEventModel insertedEvent1 = repo.insertSync(event1);

    ApplicationEventModel event2 =
        new ApplicationEventModel(application, Optional.of(actor), details);

    ApplicationEventModel insertedEvent2 = repo.insertSync(event2);

    // Evaluate.
    assertThat(insertedEvent1.id).isNotEqualTo(insertedEvent2.id);
    // Generated values.
    assertThat(insertedEvent2.id).isNotNull();
    assertThat(insertedEvent2.getCreateTime()).isAfter(startInstant);
    // Pass through values.
    assertThat(insertedEvent2.getApplication()).isEqualTo(application);
    assertThat(insertedEvent2.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(insertedEvent2.getDetails()).isEqualTo(details);
    assertThat(insertedEvent2.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
  }

  @Test
  public void getEvents() {
    // Setup
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();
    ApplicationEventModel event =
        new ApplicationEventModel(application, Optional.of(actor), details);
    repo.insertSync(event);

    // Execute
    ImmutableList<ApplicationEventModel> gotEvents =
        repo.getEventsOrderByCreateTimeDesc(application.id);

    // Verify
    assertThat(gotEvents).hasSize(1);
    ApplicationEventModel gotEvent = gotEvents.get(0);
    // Generated values.
    assertThat(gotEvent.id).isNotNull();
    assertThat(gotEvent.getCreateTime()).isAfter(startInstant);
    // Pass through values.
    assertThat(gotEvent.getApplication()).isEqualTo(application);
    assertThat(gotEvent.getCreator()).isEqualTo(Optional.of(actor));
    assertThat(gotEvent.getDetails()).isEqualTo(details);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    // NoteEvent wasn't set and should be available as an empty Optional.
    assertThat(gotEvent.getDetails().noteEvent()).isNotPresent();
  }

  @Test
  public void test_insertNoteEvent() {
    // Setup
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);

    repo.insertNoteEvent(application, ApplicationEventDetails.NoteEvent.create("some note"), actor);

    // Execute
    ImmutableList<ApplicationEventModel> applicationEvents =
        repo.getEventsOrderByCreateTimeDesc(application.id);

    // Verify
    assertThat(applicationEvents).hasSize(1);
    ApplicationEventModel firstAppEvent = applicationEvents.get(0);
    // Generated values.
    assertThat(firstAppEvent.id).isNotNull();
    assertThat(firstAppEvent.getCreateTime()).isAfter(startInstant);
    application.refresh();
    // Data is stored in application as well
    assertThat(application.getLatestNote()).isNotEmpty();
    assertThat(application.getLatestNote().get()).isEqualTo("some note");
  }

  @Test
  public void test_oldNoteOverwritten() {
    // Setup
    Instant startInstant = Instant.now();
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    AccountModel actor = resourceCreator.insertAccount();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);

    database
        .update(ApplicationModel.class)
        .set("latest_note", "initial note")
        .where()
        .eq("id", application.id)
        .update();
    application.save();
    application.refresh();

    assertThat(application.getLatestNote()).isNotEmpty();
    assertThat(application.getLatestNote().get()).isEqualTo("initial note");

    repo.insertNoteEvent(application, ApplicationEventDetails.NoteEvent.create("new note"), actor);

    // Execute
    ImmutableList<ApplicationEventModel> applicationEvents =
        repo.getEventsOrderByCreateTimeDesc(application.id);

    // Verify
    assertThat(applicationEvents).hasSize(1);
    ApplicationEventModel firstAppEvent = applicationEvents.get(0);
    // Generated values.
    assertThat(firstAppEvent.id).isNotNull();
    assertThat(firstAppEvent.getCreateTime()).isAfter(startInstant);
    application.refresh();
    // old note is rewritten
    assertThat(application.getLatestNote()).isNotEmpty();
    assertThat(application.getLatestNote().get()).isNotEqualTo("initial note");
    assertThat(application.getLatestNote().get()).isEqualTo("new note");
  }
}
