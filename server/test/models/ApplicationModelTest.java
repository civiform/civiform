package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationEventRepository;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
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
