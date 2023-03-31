package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;

public class ApplicationEventRepositoryTest extends ResetPostgres {

  private ApplicationEventRepository repo;

  @Before
  public void setUp() {
    repo = instanceOf(ApplicationEventRepository.class);
  }

  @Test
  public void insert() {
    Instant startInstant = Instant.now();
    Program program = resourceCreator.insertActiveProgram("Program");
    Account actor = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicant();
    Application application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();
    ApplicationEvent event = new ApplicationEvent(application, Optional.of(actor), details);
    ApplicationEvent insertedEvent = repo.insertSync(event);
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
    Program program = resourceCreator.insertActiveProgram("Program");
    Applicant applicant = resourceCreator.insertApplicant();
    Application application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(false).build())
            .build();
    ApplicationEvent event = new ApplicationEvent(application, Optional.empty(), details);
    ApplicationEvent insertedEvent = repo.insertAsync(event).toCompletableFuture().join();
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
  public void insertMultipleEventsOnApplication() {
    Instant startInstant = Instant.now();
    Program program = resourceCreator.insertActiveProgram("Program");
    Account actor = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicant();
    Application application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();

    ApplicationEvent event1 = new ApplicationEvent(application, Optional.of(actor), details);
    ApplicationEvent insertedEvent1 = repo.insertSync(event1);

    ApplicationEvent event2 = new ApplicationEvent(application, Optional.of(actor), details);

    ApplicationEvent insertedEvent2 = repo.insertSync(event2);

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
    Program program = resourceCreator.insertActiveProgram("Program");
    Account actor = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicant();
    Application application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder().setStatusText("Status").setEmailSent(true).build())
            .build();
    ApplicationEvent event = new ApplicationEvent(application, Optional.of(actor), details);
    repo.insertSync(event);

    // Execute
    ImmutableList<ApplicationEvent> gotEvents = repo.getEventsOrderByCreateTimeDesc(application.id);

    // Verify
    assertThat(gotEvents).hasSize(1);
    ApplicationEvent gotEvent = gotEvents.get(0);
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
}
