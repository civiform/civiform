package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationModelTest extends ResetPostgres {
  private ApplicationStatusesRepository applicationStatusesRepository;
  @Before
  public void setUp() {
    applicationStatusesRepository = instanceOf(ApplicationStatusesRepository.class);
  }

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
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .build();
applicationStatusesRepository.createOrUpdateStatusDefinitions(program.getProgramDefinition().adminName(), new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));
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
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .build();
    applicationStatusesRepository.createOrUpdateStatusDefinitions(program.getProgramDefinition().adminName(), new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));
    ApplicationModel application =
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
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .build();
    applicationStatusesRepository.createOrUpdateStatusDefinitions(program.getProgramDefinition().adminName(), new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));

    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getIsAdmin()).isFalse();
  }

  @Test
  public void isAdmin_globalAdmin_isTrue() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .build();
    applicationStatusesRepository.createOrUpdateStatusDefinitions(program.getProgramDefinition().adminName(), new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().setGlobalAdmin(true);

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }

  @Test
  public void isAdmin_programAdmin_isTrue() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .build();
    applicationStatusesRepository.createOrUpdateStatusDefinitions(program.getProgramDefinition().adminName(), new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().addAdministeredProgram(program.getProgramDefinition());

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }
}
