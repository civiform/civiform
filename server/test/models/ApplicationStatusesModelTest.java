package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationStatusesModelTest extends ResetPostgres {
  private ApplicationStatusesRepository repo;

  @Before
  public void setupApplicationStatusesModelTest() {
    repo = instanceOf(ApplicationStatusesRepository.class);
  }

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

  @Test
  public void canAddDefaultAndNonDefaultStatus() {
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("RandomTest" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions.Status nonDefaultStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("Not default")
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Not default"))
            .build();
    StatusDefinitions.Status defaultStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("Default")
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Default"))
            .setDefaultStatus(Optional.of(true))
            .build();

    repo.createOrUpdateStatusDefinitions(
        programName, new StatusDefinitions(ImmutableList.of(nonDefaultStatus)));
    assertThat(repo.lookupActiveStatusDefinitions(programName).getDefaultStatus())
        .isEqualTo(Optional.empty());

    repo.createOrUpdateStatusDefinitions(
        programName, new StatusDefinitions(ImmutableList.of(nonDefaultStatus, defaultStatus)));
    assertThat(repo.lookupActiveStatusDefinitions(programName).getDefaultStatus())
        .isEqualTo(Optional.of(defaultStatus));
  }

  private void checkApplicationStatusRow(
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
