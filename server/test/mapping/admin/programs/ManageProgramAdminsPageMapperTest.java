package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ManageProgramAdminsPageViewModel;

public final class ManageProgramAdminsPageMapperTest {

  private ManageProgramAdminsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ManageProgramAdminsPageMapper();
  }

  private ProgramDefinition createTestProgram() {
    return ProgramDefinition.builder()
        .setId(42L)
        .setAdminName("test-program")
        .setAdminDescription("A test program")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "Test Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Description"))
        .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short description"))
        .setExternalLink("")
        .setCreateTime(Instant.now())
        .setLastModifiedTime(Instant.now())
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setApplicationSteps(ImmutableList.of())
        .setBridgeDefinitions(ImmutableMap.of())
        .build();
  }

  @Test
  public void map_setsProgramName() {
    ProgramDefinition program = createTestProgram();

    ManageProgramAdminsPageViewModel result =
        mapper.map(program, ImmutableList.of(), Optional.empty());

    assertThat(result.getProgramName()).isEqualTo("test-program");
    assertThat(result.getProgramId()).isEqualTo(42L);
  }

  @Test
  public void map_buildsAdminRows() {
    ProgramDefinition program = createTestProgram();
    ImmutableList<String> emails = ImmutableList.of("admin1@example.com", "admin2@example.com");

    ManageProgramAdminsPageViewModel result = mapper.map(program, emails, Optional.empty());

    assertThat(result.getExistingAdmins()).hasSize(2);
    assertThat(result.getExistingAdmins().get(0).getEmail()).isEqualTo("admin1@example.com");
    assertThat(result.getExistingAdmins().get(1).getEmail()).isEqualTo("admin2@example.com");
  }

  @Test
  public void map_setsErrorMessage() {
    ProgramDefinition program = createTestProgram();

    ManageProgramAdminsPageViewModel result =
        mapper.map(program, ImmutableList.of(), Optional.of("Something went wrong"));

    assertThat(result.getErrorMessage()).contains("Something went wrong");
  }

  @Test
  public void map_setsUrls() {
    ProgramDefinition program = createTestProgram();

    ManageProgramAdminsPageViewModel result =
        mapper.map(program, ImmutableList.of(), Optional.empty());

    assertThat(result.getBackUrl()).isNotEmpty();
    assertThat(result.getAddAdminUrl()).isNotEmpty();
  }
}
