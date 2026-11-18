package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Optional;
import models.ApplicationStep;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ManageProgramAdminsPageViewModel;

public final class ManageProgramAdminsPageMapperTest {

  private static final ImmutableList<String> ADMIN_EMAILS =
      ImmutableList.of("one@example.com", "two@example.com");

  private ManageProgramAdminsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ManageProgramAdminsPageMapper();
  }

  private static ProgramDefinition program() {
    return ProgramDefinition.builder()
        .setId(7L)
        .setAdminName("utility-discount")
        .setAdminDescription("")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Utility Discount"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("Help with utilities"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setLastModifiedTime(Instant.parse("2024-01-01T17:00:00Z"))
        .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
        .setBridgeDefinitions(ImmutableMap.of())
        .build();
  }

  @Test
  public void map_setsProgramAdminName() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, /* errorMessage= */ Optional.empty());

    assertThat(result.getProgramAdminName()).isEqualTo("utility-discount");
  }

  @Test
  public void map_setsBackUrl() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, /* errorMessage= */ Optional.empty());

    assertThat(result.getBackUrl()).isEqualTo("/admin/programs");
  }

  @Test
  public void map_setsAddAdminUrl() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, /* errorMessage= */ Optional.empty());

    assertThat(result.getAddAdminUrl()).isEqualTo("/admin/programs/7/admins/add");
  }

  @Test
  public void map_setsDeleteAdminUrl() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, /* errorMessage= */ Optional.empty());

    assertThat(result.getDeleteAdminUrl()).isEqualTo("/admin/programs/7/admins/delete");
  }

  @Test
  public void map_setsExistingAdminEmailsInOrder() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, /* errorMessage= */ Optional.empty());

    assertThat(result.getExistingAdminEmails())
        .containsExactly("one@example.com", "two@example.com");
  }

  @Test
  public void map_noError_leavesToastUnset() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, /* errorMessage= */ Optional.empty());

    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getErrorToastId()).isNull();
  }

  @Test
  public void map_error_prefixesMessage() {
    ManageProgramAdminsPageViewModel result =
        mapper.map(program(), ADMIN_EMAILS, Optional.of("Enter an admin email"));

    assertThat(result.getErrorMessage()).isEqualTo("Error: Enter an admin email");
  }

  @Test
  public void map_error_setsRandomToastId() {
    ManageProgramAdminsPageViewModel first =
        mapper.map(program(), ADMIN_EMAILS, Optional.of("Enter an admin email"));
    ManageProgramAdminsPageViewModel second =
        mapper.map(program(), ADMIN_EMAILS, Optional.of("Enter an admin email"));

    assertThat(first.getErrorToastId()).isNotBlank();
    assertThat(first.getErrorToastId()).isNotEqualTo(second.getErrorToastId());
  }
}
