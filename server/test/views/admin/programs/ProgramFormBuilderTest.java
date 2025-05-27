package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import repository.AccountRepository;
import repository.CategoryRepository;
import services.program.ProgramType;
import services.settings.SettingsManifest;

public class ProgramFormBuilderTest {
  private ProgramFormBuilder formBuilder;
  private Config config;

  @Before
  public void setup() {
    config = ConfigFactory.load();
    SettingsManifest settingsManifest = mock(SettingsManifest.class);
    AccountRepository mockAccountRepo = mock(AccountRepository.class);
    CategoryRepository mockCategoryRepo = mock(CategoryRepository.class);
    formBuilder =
        new ProgramFormBuilder(config, settingsManifest, mockAccountRepo, mockCategoryRepo) {};
  }

  @Test
  public void buildApplicationStepDiv_buildsApplicationStepFormElement() {
    DivTag applicationStepsDiv =
        ProgramFormBuilder.buildApplicationStepDiv(0, ImmutableList.of(), /* isDisabled= */ false);
    String renderedDiv = applicationStepsDiv.render();

    // field id
    assertThat(renderedDiv).contains("apply-step-1-title");
    assertThat(renderedDiv).contains("apply-step-1-description");
    // field name
    assertThat(renderedDiv).contains("applicationSteps[0][title]");
    assertThat(renderedDiv).contains("applicationSteps[0][description]");
    // field label
    assertThat(renderedDiv).contains("Step 1 title");
    assertThat(renderedDiv).contains("Step 1 description");

    DivTag optionalApplicationStepsDiv =
        ProgramFormBuilder.buildApplicationStepDiv(1, ImmutableList.of(), /* isDisabled= */ false);
    String renderedOptionalDiv = optionalApplicationStepsDiv.render();

    // field label for divs other than the first one are labeled "optional"
    assertThat(renderedOptionalDiv).contains("Step 2 title (optional)");
    assertThat(renderedOptionalDiv).contains("Step 2 description (optional)");
  }

  @Test
  public void buildApplicationStepDiv_rendersExistingValues() {
    DivTag applicationStepDiv =
        ProgramFormBuilder.buildApplicationStepDiv(
            0,
            ImmutableList.of(
                Map.of("title", "Step one title", "description", "Step one description")),
            /* isDisabled= */ false);
    String renderedDiv = applicationStepDiv.render();

    assertThat(renderedDiv).contains("Step one title");
    assertThat(renderedDiv).contains("Step one description");
  }

  @Test
  public void buildProgramSlugField_creationStatus_externalProgramCardsFeatureEnabled() {
    DomContent urlFieldResult =
        formBuilder.buildProgramSlugFieldForExternalProgramsFeature(
            "test-program", ProgramEditStatus.CREATION, ProgramType.DEFAULT);
    String urlFieldRendered = urlFieldResult.render();
    assertThat(urlFieldRendered)
        .contains(
            " Create a program ID. This ID can only contain lowercase letters, numbers, and dashes."
                + " It will be used in the program’s applicant-facing URL (except for external"
                + " programs), and it can’t be changed later.");
  }

  @Test
  public void buildProgramSlugField_editStatus_DefaultProgram_externalProgramCardsFeatureEnabled() {
    String baseUrl = config.getString("base_url");
    DomContent urlFieldResult =
        formBuilder.buildProgramSlugFieldForExternalProgramsFeature(
            "test-program", ProgramEditStatus.EDIT, ProgramType.DEFAULT);
    String urlFieldRendered = urlFieldResult.render();
    assertThat(urlFieldRendered).contains("The URL for this program. This URL can’t be changed");
    assertThat(urlFieldRendered).contains(baseUrl + "/programs/test-program");
  }

  @Test
  public void
      buildProgramSlugField_editStatus_ExternalProgram_externalProgramCardsFeatureEnabled() {
    DomContent urlFieldResult =
        formBuilder.buildProgramSlugFieldForExternalProgramsFeature(
            "test-program", ProgramEditStatus.EDIT, ProgramType.EXTERNAL);
    String urlFieldRendered = urlFieldResult.render();
    assertThat(urlFieldRendered).contains("The program ID. This ID can’t be changed.");
    assertThat(urlFieldRendered).contains("test-program");
  }
}
