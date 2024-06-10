package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import models.ApplicationStatusesModel;
import models.ProgramModel;
import models.StatusLifecycleStage;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationStatusesRepositoryTest extends ResetPostgres {
  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .build();
  ApplicationStatusesRepository repo;

  @Before
  public void setupStatusRepository() {
    repo = instanceOf(ApplicationStatusesRepository.class);
  }

  @Test
  public void canQueryForActiveApplicationStatuses() {
    // setup
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Active status tests" + uniqueProgramId, "description")
            .build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    // test
    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(programName, statusDefinitions, StatusLifecycleStage.ACTIVE);
    applicationStatusesModel.save();
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    StatusDefinitions statusDefinitionsResult = repo.lookupActiveStatuDefinitions(programName);
    // assert
    assertThat(statusDefinitionsResult.getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResult.getStatuses().get(0).statusText()).isEqualTo("Approved");
  }

  @Test
  public void canQueryForActiveApplicationStatuses_throwsException() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> repo.lookupActiveStatuDefinitions("random"))
        .withMessage("No active status found for program random");
  }

  @Test
  public void canQueryForObsoleteApplicationStatuses_throwsException() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> repo.lookupListOfObsoleteStatusDefinitions("random"))
        .withMessage("No obsolete status found for program random");
  }

  @Test
  public void canQueryForOboseleteApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));

    ApplicationStatusesModel applicationStatusesModel =
        new ApplicationStatusesModel(programName, statusDefinitions, StatusLifecycleStage.OBSOLETE);
    applicationStatusesModel.save();
    applicationStatusesModel.setCreateTimeForTest("2041-01-01T00:00:00Z").save();
    List<StatusDefinitions> statusDefinitionsResults =
        repo.lookupListOfObsoleteStatusDefinitions(programName);

    assertThat(statusDefinitionsResults).isNotEmpty();
    assertThat(statusDefinitionsResults.size()).isEqualTo(1);
    assertThat(statusDefinitionsResults.get(0).getStatuses().get(0).statusText())
        .isEqualTo("Approved");
  }
}
