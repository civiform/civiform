package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.program.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationTest extends ResetPostgres {

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .build();

  @Test
  public void staleLatestStatusIsNotPersisted() {
    // Tests a case where an Application (and its associated latest_status value has been loaded
    // in-memory, a new ApplicationEventDetails is added (causing the trigger to execute), and the
    // Application is persisted.
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    ApplicationEvent event =
        new ApplicationEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.builder()
                .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
                .setStatusEvent(
                    StatusEvent.builder()
                        .setStatusText(APPROVED_STATUS.statusText())
                        .setEmailSent(false)
                        .build())
                .build());
    event.save();

    // Trigger an update in the application.
    application.setSubmitTimeToNow();
    application.setLatestStatusForTest("some-status-value");
    application.save();
    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of(APPROVED_STATUS.statusText()));
  }

  @Test
  public void latestStatusIsNotPersistedEvenWithNoApplicationEvents() {
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Application application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    application.setLatestStatusForTest("some-status-value");
    application.save();
    application.refresh();
    assertThat(application.getLatestStatus()).isEmpty();
  }

  @Test
  public void isAdmin_applicant_isFalse() {
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Application application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getIsAdmin()).isFalse();
  }

  @Test
  public void isAdmin_globalAdmin_isTrue() {
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().setGlobalAdmin(true);

    Application application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }

  @Test
  public void isAdmin_programAdmin_isTrue() {
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().addAdministeredProgram(program.getProgramDefinition());

    Application application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }
}
