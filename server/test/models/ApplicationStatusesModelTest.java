package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Random;
import org.junit.Test;
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

  @Test
  public void canAddActiveApplicationStatuses() {
    // setup
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Obsolete tests" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(
            programName, statusDefinitions, StatusDefinitionsLifecycleStage.ACTIVE);
    applicationStatusesModel.save();
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    // test
    applicationStatusesModel.refresh();
    // assert
    assertThat(applicationStatusesModel).isNotNull();
    checkApplicationStatusRow(
        applicationStatusesModel, programName, "Approved", StatusDefinitionsLifecycleStage.ACTIVE);
  }

  @Test
  public void canAddOboseleteApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(
            programName, statusDefinitions, StatusDefinitionsLifecycleStage.OBSOLETE);
    applicationStatusesModel.save();
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();

    applicationStatusesModel.refresh();

    assertThat(applicationStatusesModel).isNotNull();
    checkApplicationStatusRow(
        applicationStatusesModel,
        programName,
        "Approved",
        StatusDefinitionsLifecycleStage.OBSOLETE);
  }

  public void checkApplicationStatusRow(
      ApplicationStatusesModel applicationStatusesModel,
      String programName,
      String statusText,
      StatusDefinitionsLifecycleStage statusDefinitionsLifecycleStage) {
    assertThat(applicationStatusesModel.getCreateTime()).isEqualTo("2041-01-01T00:00:00Z");
    assertThat(applicationStatusesModel.getStatusDefinitions().getStatuses().get(0).statusText())
        .isEqualTo(statusText);
    assertThat(applicationStatusesModel.getProgramName()).isEqualTo(programName);
    assertThat(applicationStatusesModel.getStatusDefinitionsLifecycleStage())
        .isEqualTo(statusDefinitionsLifecycleStage);
  }
}
