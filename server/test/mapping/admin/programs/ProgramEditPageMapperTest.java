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
import views.admin.programs.ProgramEditPageViewModel;
import views.admin.programs.ProgramEditStatus;

public final class ProgramEditPageMapperTest {

  private ProgramEditPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramEditPageMapper();
  }

  private ProgramDefinition buildProgram(ProgramType programType) {
    return ProgramDefinition.builder()
        .setId(1L)
        .setAdminName("test-program")
        .setAdminDescription("admin desc")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "Test Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Description"))
        .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short Desc"))
        .setExternalLink("http://example.com")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(programType)
        .setLocalizedConfirmationMessage(LocalizedStrings.of(Locale.US, "Confirmed"))
        .setAcls(new ProgramAcls())
        .setBlockDefinitions(ImmutableList.of())
        .build();
  }

  @Test
  public void map_setsProgramName() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getProgramName()).isEqualTo("Test Program");
  }

  @Test
  public void map_setsProgramId() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getProgramId()).isEqualTo(1L);
  }

  @Test
  public void map_setsFormActionUrl() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_defaultProgram_setsTypeFlags() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.isDefaultProgram()).isTrue();
    assertThat(result.isPreScreenerForm()).isFalse();
    assertThat(result.isExternalProgram()).isFalse();
  }

  @Test
  public void map_externalProgram_setsTypeFlags() {
    ProgramDefinition program = buildProgram(ProgramType.EXTERNAL);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            true,
            "http://localhost",
            Optional.empty());

    assertThat(result.isDefaultProgram()).isFalse();
    assertThat(result.isExternalProgram()).isTrue();
    assertThat(result.getProgramUrl()).isEmpty();
  }

  @Test
  public void map_defaultProgram_setsManageQuestionsUrl() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getManageQuestionsUrl()).isPresent();
  }

  @Test
  public void map_externalProgram_hasNoManageQuestionsUrl() {
    ProgramDefinition program = buildProgram(ProgramType.EXTERNAL);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getManageQuestionsUrl()).isEmpty();
  }

  @Test
  public void map_setsPrePopulatedFields() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getAdminName()).isEqualTo("test-program");
    assertThat(result.getAdminDescription()).isEqualTo("admin desc");
    assertThat(result.getDisplayName()).isEqualTo("Test Program");
    assertThat(result.getShortDescription()).isEqualTo("Short Desc");
    assertThat(result.getExternalLink()).isEqualTo("http://example.com");
  }

  @Test
  public void map_setsEmptyErrorMessage() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.empty());

    assertThat(result.getErrorMessage()).isEmpty();
  }

  @Test
  public void map_setsErrorMessage() {
    ProgramDefinition program = buildProgram(ProgramType.DEFAULT);

    ProgramEditPageViewModel result =
        mapper.map(
            program,
            ProgramEditStatus.EDIT,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            "http://localhost",
            Optional.of("some error"));

    assertThat(result.getErrorMessage()).contains("some error");
  }
}
