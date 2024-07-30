package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.google.common.collect.ImmutableList;
import java.util.Random;
import models.ApplicationStatusesModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;

public class ApplicationStatusesRepositoryTest extends ResetPostgres {
  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .build();
  private static final StatusDefinitions.Status REAPPLY_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Reapply")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Reapply"))
          .build();
  ApplicationStatusesRepository repo;

  @Before
  public void setupStatusRepository() {
    repo = instanceOf(ApplicationStatusesRepository.class);
  }

  @Test
  public void canQueryForActiveApplicationStatuses() {

    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Active status tests" + uniqueProgramId, "description")
            .build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)));

    StatusDefinitions statusDefinitionsResult =
        repo.lookupActiveStatusDefinitions(program.getProgramDefinition().adminName());

    assertThat(statusDefinitionsResult.getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResult.getStatuses().get(0).statusText()).isEqualTo("Approved");
  }

  @Test
  public void lookupActiveStatusDefinitions_throwsException() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> repo.lookupActiveStatusDefinitions("random"))
        .withMessage("No active status found for program random");
  }

  @Test
  public void lookupListOfObsoleteStatusDefinitions_throwsException() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> repo.lookupAllApplicationStatusesModels("random"))
        .withMessage("No statuses found for the program random");
  }

  @Test
  public void canQueryForAllApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program" + uniqueProgramId, "description").build();

    String programName = program.getProgramDefinition().adminName();
    repo.createOrUpdateStatusDefinitions(
        programName, new StatusDefinitions(ImmutableList.of(REAPPLY_STATUS)));
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    repo.createOrUpdateStatusDefinitions(programName, statusDefinitions);

    ImmutableList<ApplicationStatusesModel> statusDefinitionsModelResults =
        repo.lookupAllApplicationStatusesModels(programName);

    assertThat(statusDefinitionsModelResults).isNotEmpty();
    // one status is added as part of the program creation and one status as obsolete status
    assertThat(statusDefinitionsModelResults.size()).isEqualTo(4);
    assertThat(statusDefinitionsModelResults.get(0).getStatusDefinitions().getStatuses().size())
        .isEqualTo(1);
    assertThat(
            statusDefinitionsModelResults
                .get(0)
                .getStatusDefinitions()
                .getStatuses()
                .get(0)
                .statusText())
        .isEqualTo("Reapply");
    assertThat(
            statusDefinitionsModelResults
                .get(2)
                .getStatusDefinitions()
                .getStatuses()
                .get(0)
                .statusText())
        .isEqualTo("Approved");
  }

  @Test
  public void canUpdateApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Updateprogram" + uniqueProgramId, "description").build();
    String programName = program.getProgramDefinition().adminName();
    repo.createOrUpdateStatusDefinitions(programName, statusDefinitions);

    StatusDefinitions statusDefinitionsResult = repo.lookupActiveStatusDefinitions(programName);
    assertThat(statusDefinitionsResult.getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResult.getStatuses().get(0).statusText()).isEqualTo("Approved");

    repo.createOrUpdateStatusDefinitions(
        programName, new StatusDefinitions(ImmutableList.of(REAPPLY_STATUS)));

    StatusDefinitions statusDefinitionsResult2 = repo.lookupActiveStatusDefinitions(programName);
    assertThat(statusDefinitionsResult2.getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResult2.getStatuses().get(0).statusText()).isEqualTo("Reapply");
  }
}
