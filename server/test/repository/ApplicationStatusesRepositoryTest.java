package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import models.ProgramModel;
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
    // setup
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Active status tests" + uniqueProgramId, "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();
    // test
    StatusDefinitions statusDefinitionsResult =
        repo.lookupActiveStatusDefinitions(program.getProgramDefinition().adminName());
    // assert
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
        .isThrownBy(() -> repo.lookupListOfObsoleteStatusDefinitions("random"))
        .withMessage("No obsolete status found for program random");
  }

  @Test
  public void canQueryForOboseleteApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program" + uniqueProgramId, "description")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(REAPPLY_STATUS)))
            .build();

    String programName = program.getProgramDefinition().adminName();

    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    repo.createOrUpdateStatusDefinitions(programName, statusDefinitions);

    List<StatusDefinitions> statusDefinitionsResults =
        repo.lookupListOfObsoleteStatusDefinitions(programName);

    assertThat(statusDefinitionsResults).isNotEmpty();
    // one status is added as part of the program creation and one status as obsolete status
    assertThat(statusDefinitionsResults.size()).isEqualTo(2);
    assertThat(statusDefinitionsResults.get(0).getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResults.get(0).getStatuses().get(0).statusText())
        .isEqualTo("Reapply");
  }

  @Test
  public void canUpdateApplicationStatuses() {
    Long uniqueProgramId = new Random().nextLong();
    StatusDefinitions statusDefinitions = new StatusDefinitions(ImmutableList.of(APPROVED_STATUS));
    ProgramModel program =
        ProgramBuilder.newActiveProgram("Updateprogram" + uniqueProgramId, "description")
            .withStatusDefinitions(statusDefinitions)
            .build();
    String programName = program.getProgramDefinition().adminName();

    // pre assert before test
    StatusDefinitions statusDefinitionsResult = repo.lookupActiveStatusDefinitions(programName);
    assertThat(statusDefinitionsResult.getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResult.getStatuses().get(0).statusText()).isEqualTo("Approved");
    // test
    repo.createOrUpdateStatusDefinitions(
        programName, new StatusDefinitions(ImmutableList.of(REAPPLY_STATUS)));

    StatusDefinitions statusDefinitionsResult2 = repo.lookupActiveStatusDefinitions(programName);
    assertThat(statusDefinitionsResult2.getStatuses().size()).isEqualTo(1);
    assertThat(statusDefinitionsResult2.getStatuses().get(0).statusText()).isEqualTo("Reapply");
  }
}
