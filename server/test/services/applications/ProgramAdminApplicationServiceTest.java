package services.applications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class ProgramAdminApplicationServiceTest extends ResetPostgres {
  private ProgramAdminApplicationService service;

  @Before
  public void setProgramServiceImpl() {
    service = instanceOf(ProgramAdminApplicationService.class);
  }

  @Test
  public void getApplication() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("some-program").buildDefinition();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    Optional<Application> result = service.getApplication(application.id, program);
    assertThat(result).isPresent();
    assertThat(result.get().id).isEqualTo(application.id);
  }

  @Test
  public void getApplication_notFound() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram().buildDefinition();
    assertThat(service.getApplication(Long.MAX_VALUE, program)).isEmpty();
  }

  @Test
  public void getApplication_programMismatch() {
    ProgramDefinition firstProgram =
        ProgramBuilder.newActiveProgram("first-program").buildDefinition();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application firstProgramApplication =
        Application.create(applicant, firstProgram.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    ProgramDefinition secondProgram =
        ProgramBuilder.newActiveProgram("second-program").buildDefinition();

    assertThat(service.getApplication(firstProgramApplication.id, secondProgram)).isEmpty();
  }

  @Test
  public void getApplication_emptyAdminName() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("").buildDefinition();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    assertThat(service.getApplication(application.id, program)).isEmpty();
  }
}
