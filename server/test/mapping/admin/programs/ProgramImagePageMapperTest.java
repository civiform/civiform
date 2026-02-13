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
import views.admin.programs.ProgramImagePageViewModel;

public final class ProgramImagePageMapperTest {

  private ProgramImagePageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramImagePageMapper();
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
  public void map_setsBackUrlForEditStatus() {
    ProgramDefinition program = createTestProgram();

    ProgramImagePageViewModel result =
        mapper.map(
            program,
            "EDIT",
            false,
            "http://upload",
            "upload-class",
            ImmutableMap.of(),
            10,
            "<p>preview</p>",
            Optional.empty(),
            Optional.empty());

    assertThat(result.getBackUrl()).isNotEmpty();
    assertThat(result.isShowContinueButton()).isFalse();
  }

  @Test
  public void map_setsBackUrlForCreationStatus() {
    ProgramDefinition program = createTestProgram();

    ProgramImagePageViewModel result =
        mapper.map(
            program,
            "CREATION",
            false,
            "http://upload",
            "upload-class",
            ImmutableMap.of(),
            10,
            "<p>preview</p>",
            Optional.empty(),
            Optional.empty());

    assertThat(result.isShowContinueButton()).isTrue();
  }

  @Test
  public void map_setsFileUploadFields() {
    ProgramDefinition program = createTestProgram();
    ImmutableMap<String, String> formFields = ImmutableMap.of("key", "value");

    ProgramImagePageViewModel result =
        mapper.map(
            program,
            "EDIT",
            false,
            "http://upload-action",
            "form-class",
            formFields,
            25,
            "<p>preview</p>",
            Optional.empty(),
            Optional.empty());

    assertThat(result.getImageUploadFormAction()).isEqualTo("http://upload-action");
    assertThat(result.getImageUploadFormClass()).isEqualTo("form-class");
    assertThat(result.getImageUploadFormFields()).isEqualTo(formFields);
    assertThat(result.getFileLimitMb()).isEqualTo(25);
  }

  @Test
  public void map_setsFlashMessages() {
    ProgramDefinition program = createTestProgram();

    ProgramImagePageViewModel result =
        mapper.map(
            program,
            "EDIT",
            false,
            "",
            "",
            ImmutableMap.of(),
            10,
            "",
            Optional.of("Success!"),
            Optional.of("Error!"));

    assertThat(result.getSuccessMessage()).contains("Success!");
    assertThat(result.getErrorMessage()).contains("Error!");
  }

  @Test
  public void map_withTranslatableLocales_setsManageTranslationsUrl() {
    ProgramDefinition program = createTestProgram();

    ProgramImagePageViewModel result =
        mapper.map(
            program,
            "EDIT",
            true,
            "",
            "",
            ImmutableMap.of(),
            10,
            "",
            Optional.empty(),
            Optional.empty());

    assertThat(result.getManageTranslationsUrl()).isPresent();
  }

  @Test
  public void map_withoutTranslatableLocales_noManageTranslationsUrl() {
    ProgramDefinition program = createTestProgram();

    ProgramImagePageViewModel result =
        mapper.map(
            program,
            "EDIT",
            false,
            "",
            "",
            ImmutableMap.of(),
            10,
            "",
            Optional.empty(),
            Optional.empty());

    assertThat(result.getManageTranslationsUrl()).isEmpty();
  }
}
