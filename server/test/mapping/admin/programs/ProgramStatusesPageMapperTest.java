package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramStatusesPageViewModel;

public final class ProgramStatusesPageMapperTest {

  private ProgramStatusesPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramStatusesPageMapper();
  }

  private ProgramDefinition buildProgram() {
    return ProgramDefinition.builder()
        .setId(1L)
        .setAdminName("test-program")
        .setAdminDescription("desc")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "Test Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Desc"))
        .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setLocalizedConfirmationMessage(LocalizedStrings.empty())
        .setAcls(new ProgramAcls())
        .setBlockDefinitions(ImmutableList.of())
        .build();
  }

  @Test
  public void map_setsProgramName() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramStatusesPageViewModel result =
        mapper.map(program, statusDefinitions, false, Optional.empty(), Optional.empty());

    assertThat(result.getProgramName()).isEqualTo("Test Program");
  }

  @Test
  public void map_setsProgramId() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramStatusesPageViewModel result =
        mapper.map(program, statusDefinitions, false, Optional.empty(), Optional.empty());

    assertThat(result.getProgramId()).isEqualTo(1L);
  }

  @Test
  public void map_setsStatusesFromDefinitions() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions =
        new StatusDefinitions(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("Approved")
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
                    .setLocalizedEmailBodyText(
                        Optional.of(LocalizedStrings.withDefaultValue("You are approved")))
                    .setDefaultStatus(Optional.of(false))
                    .build(),
                StatusDefinitions.Status.builder()
                    .setStatusText("Denied")
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Denied"))
                    .setLocalizedEmailBodyText(Optional.empty())
                    .setDefaultStatus(Optional.of(false))
                    .build()));

    ProgramStatusesPageViewModel result =
        mapper.map(program, statusDefinitions, false, Optional.empty(), Optional.empty());

    assertThat(result.getStatuses()).hasSize(2);
    assertThat(result.getStatuses().get(0).getStatusText()).isEqualTo("Approved");
    assertThat(result.getStatuses().get(0).isHasEmail()).isTrue();
    assertThat(result.getStatuses().get(1).getStatusText()).isEqualTo("Denied");
    assertThat(result.getStatuses().get(1).isHasEmail()).isFalse();
  }

  @Test
  public void map_withTranslatableLocales_setsManageTranslationsUrl() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramStatusesPageViewModel result =
        mapper.map(program, statusDefinitions, true, Optional.empty(), Optional.empty());

    assertThat(result.getManageTranslationsUrl()).isPresent();
  }

  @Test
  public void map_withoutTranslatableLocales_emptyManageTranslationsUrl() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramStatusesPageViewModel result =
        mapper.map(program, statusDefinitions, false, Optional.empty(), Optional.empty());

    assertThat(result.getManageTranslationsUrl()).isEmpty();
  }

  @Test
  public void map_setsSuccessAndErrorMessages() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramStatusesPageViewModel result =
        mapper.map(
            program, statusDefinitions, false, Optional.of("Success!"), Optional.of("Error!"));

    assertThat(result.getSuccessMessage()).contains("Success!");
    assertThat(result.getErrorMessage()).contains("Error!");
  }

  @Test
  public void map_setsModalIds() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions =
        new StatusDefinitions(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("Approved")
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
                    .setLocalizedEmailBodyText(Optional.empty())
                    .setDefaultStatus(Optional.of(false))
                    .build()));

    ProgramStatusesPageViewModel result =
        mapper.map(program, statusDefinitions, false, Optional.empty(), Optional.empty());

    assertThat(result.getStatuses().get(0).getModalId()).isEqualTo("edit-status-modal-0");
    assertThat(result.getStatuses().get(0).getDeleteModalId()).isEqualTo("delete-status-modal-0");
  }
}
