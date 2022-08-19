package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
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
  public void insertAndGet() {
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
    ApplicationEvent event =
        new ApplicationEvent(application, actor, ApplicationEventDetails.Type.STATUS_CHANGE, details);
    repo.insert(event).toCompletableFuture().join();

    ImmutableList<ApplicationEvent> events =
        repo.getEvents(application.id).toCompletableFuture().join();
    assertThat(events).hasSize(1);
    ApplicationEvent gotEvent = events.get(0);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    assertThat(gotEvent.getActor()).isEqualTo(actor);
    assertThat(gotEvent.getDetails()).isEqualTo(details);
    // NoteEvent wasn't set and should be available as an empty Optional.
    assertThat(gotEvent.getDetails().noteEvent()).isNotPresent();
  }
}
