package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationEventRepository;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.application.ApplicationEventDetails;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationModelTest extends ResetPostgres {

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .build();
  private ApplicationStatusesRepository applicationStatusesRepository;
  private ApplicationEventRepository applicationEventRepository;

  @Before
  public void setUp() {
    applicationStatusesRepository = instanceOf(ApplicationStatusesRepository.class);
    applicationEventRepository = instanceOf(ApplicationEventRepository.class);
  }

  @Test
  public void latestStatusIsCarriedForwardEvenAfterApplicationUpdates() {
    // Tests a case where an Application's status is preserved, even if the application row is
    // updated
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();
    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));

    AccountModel adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getLatestStatus()).isEmpty();

    applicationEventRepository
        .insertStatusEvent(
            application,
            Optional.of(adminAccount),
            ApplicationEventDetails.StatusEvent.builder()
                .setStatusText(APPROVED_STATUS.statusText())
                .setEmailSent(false)
                .build())
        .toCompletableFuture()
        .join();

    // Trigger an update in the application.
    application.setSubmitTimeToNow();
    applicationEventRepository.insertNoteEvent(
        application, ApplicationEventDetails.NoteEvent.create("some note"), adminAccount);
    application.refresh();
    assertThat(application.getLatestStatus()).isEqualTo(Optional.of(APPROVED_STATUS.statusText()));
  }

  @Test
  public void eligibility_determination_default_not_computed() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(
        application.getEligibilityDetermination().equals(EligibilityDetermination.NOT_COMPUTED));
  }

  @Test
  public void eligibility_determination_eligible() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.ELIGIBLE);
    assertThat(application.getEligibilityDetermination().equals(EligibilityDetermination.ELIGIBLE));
  }

  @Test
  public void eligibility_determination_ineligible() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.INELIGIBLE);
    assertThat(
        application.getEligibilityDetermination().equals(EligibilityDetermination.INELIGIBLE));
  }

  @Test
  public void eligibility_determination_no_eligibility_criteria() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
    assertThat(
        application
            .getEligibilityDetermination()
            .equals(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA));
  }

  @Test
  public void isAdmin_applicant_isFalse() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getIsAdmin()).isFalse();
  }

  @Test
  public void isAdmin_globalAdmin_isTrue() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().setGlobalAdmin(true);

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }

  @Test
  public void isAdmin_programAdmin_isTrue() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();

    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().addAdministeredProgram(program.getProgramDefinition());

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }
}
