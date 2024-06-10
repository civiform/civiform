package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationStatusDefinitionsRepository;
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
  ApplicationStatusDefinitionsRepository repo;

  @Before
  public void setupStatusRepository() {
    repo = instanceOf(ApplicationStatusDefinitionsRepository.class);
  }

  @Test
  public void canAddActiveApplicationStatuses() {
    // setup
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Obsolete tests" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    // test
    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(programName, statusDefinitions, StatusLifecycleStage.ACTIVE);
    applicationStatusesModel.save();
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    Optional<ApplicationStatusesModel> mayBeStatus = repo.lookupActiveStatuDefinitions(programName);
    // assert
    assertThat(mayBeStatus).isNotEmpty();
    ApplicationStatusesModel applicationStatusesModel1 = mayBeStatus.get();
    checkApplicationStatusRow(
        applicationStatusesModel1, programName, "Approved", StatusLifecycleStage.ACTIVE);
  }

  @Test
  public void canAddOboseleteApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));

    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(programName, statusDefinitions, StatusLifecycleStage.OBSOLETE);
    applicationStatusesModel.save();
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    List<ApplicationStatusesModel> mayBeStatuses =
        repo.lookupListOfObsoleteStatusDefinitions(programName);

    assertThat(mayBeStatuses).isNotEmpty();
    assertThat(mayBeStatuses.size()).isEqualTo(1);
    ApplicationStatusesModel applicationStatusesModel1 = mayBeStatuses.get(0);
    checkApplicationStatusRow(
        applicationStatusesModel1, programName, "Approved", StatusLifecycleStage.OBSOLETE);
  }

  public void checkApplicationStatusRow(
      ApplicationStatusesModel applicationStatusesModel,
      String programName,
      String statusText,
      StatusLifecycleStage statusLifecycleStage) {
    assertThat(applicationStatusesModel.getCreateTime()).isEqualTo("2041-01-01T00:00:00Z");
    assertThat(applicationStatusesModel.getStatusDefinitions().getStatuses().get(0).statusText())
        .isEqualTo(statusText);
    assertThat(applicationStatusesModel.getProgramName()).isEqualTo(programName);
    assertThat(applicationStatusesModel.getStatusLifecycleStage()).isEqualTo(statusLifecycleStage);
  }
}
