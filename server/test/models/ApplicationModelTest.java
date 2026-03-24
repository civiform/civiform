package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
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
  private ProgramModel program;

  @Before
  public void setUp() {
    applicationStatusesRepository = instanceOf(ApplicationStatusesRepository.class);
    applicationEventRepository = instanceOf(ApplicationEventRepository.class);
    program = ProgramBuilder.newActiveProgram("test program", "description").build();
  }

  @Test
  public void latestStatusIsCarriedForwardEvenAfterApplicationUpdates() {
    // Tests a case where an Application's status is preserved, even if the application row is
    // updated
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
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NOT_COMPUTED);
  }

  @Test
  public void eligibility_determination_eligible() {
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.ELIGIBLE);
    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.ELIGIBLE);
  }

  @Test
  public void eligibility_determination_ineligible() {
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.INELIGIBLE);
    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.INELIGIBLE);
  }

  @Test
  public void eligibility_determination_no_eligibility_criteria() {
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
  }

  @Test
  public void isAdmin_applicant_isFalse() {
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
    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getAccount().addAdministeredProgram(program.getProgramDefinition());

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    assertThat(application.getIsAdmin()).isTrue();
  }

  @Test
  public void originalApplicantId_defaultsToEmpty() {
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    assertThat(application.getOriginalApplicantId()).isEmpty();
    application.save();

    application.refresh();
    assertThat(application.getOriginalApplicantId()).isEmpty();
  }

  @Test
  public void originalApplicantId_canBeSetAndPersisted() {
    long applicantId = 500L;
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setOriginalApplicantId(applicantId);
    application.save();

    application.refresh();
    assertThat(application.getOriginalApplicantId()).isEqualTo(Optional.of(applicantId));
  }

  @Test
  public void create_new_application_updatesLastActivityTime() {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    Instant activitytimeBeforeUpdate = applicant.getAccount().getLastActivityTime();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    application.refresh();

    Instant activitytimeAfterUpdate = applicant.getAccount().getLastActivityTime();

    assertThat(activitytimeAfterUpdate).isAfter(activitytimeBeforeUpdate);
  }
}
