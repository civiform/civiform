package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationStatusRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationStatusesModelTest extends ResetPostgres {
  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .build();
  ApplicationStatusRepository repo;

  @Before
  public void setupStatusRepository() {
    repo = instanceOf(ApplicationStatusRepository.class);
  }

  @Test
  public void canAddActiveApplicationStatuses() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(programName, statusDefinitions, StatusLifecycleStage.ACTIVE);
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    Optional<ApplicationStatusesModel> mayBeStatus = repo.lookupActiveStatus(programName);
    assertThat(mayBeStatus).isNotEmpty();
    ApplicationStatusesModel applicationStatusesModel1 = mayBeStatus.get();
    assertThat(applicationStatusesModel1.getCreateTime()).isEqualTo("2041-01-01T00:00:00Z");
    assertThat(applicationStatusesModel1.getStatusDefinitions()).isEqualTo(statusDefinitions);
    assertThat(applicationStatusesModel1.getProgramName()).isEqualTo(programName);
    assertThat(applicationStatusesModel1.getStatusLifecycleStage())
        .isEqualTo(StatusLifecycleStage.ACTIVE);
  }

  @Test
  public void canAddOboseleteApplicationStatuses() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(programName, statusDefinitions, StatusLifecycleStage.OBSOLETE);
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    Optional<ApplicationStatusesModel> mayBeStatus = repo.lookupActiveStatus(programName);
    assertThat(mayBeStatus).isNotEmpty();
    ApplicationStatusesModel applicationStatusesModel1 = mayBeStatus.get();
    assertThat(applicationStatusesModel1.getCreateTime()).isEqualTo("2041-01-01T00:00:00Z");
    assertThat(applicationStatusesModel1.getStatusDefinitions()).isEqualTo(statusDefinitions);
    assertThat(applicationStatusesModel1.getProgramName()).isEqualTo(programName);
    assertThat(applicationStatusesModel1.getStatusLifecycleStage())
        .isEqualTo(StatusLifecycleStage.OBSOLETE);
  }
}
