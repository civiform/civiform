package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import play.i18n.MessagesApi;
import repository.AccountRepository;
import repository.CategoryRepository;
import repository.ResetPostgres;
import services.program.ProgramType;
import services.settings.SettingsManifest;

public class ProgramFormBuilderTest extends ResetPostgres {
  private ProgramFormBuilder formBuilder;
  private Config config;

  /**
   * Counts the number of required indicator spans that are hidden in the rendered div.
   *
   * @param renderedDiv the HTML string to search for hidden required indicator spans
   * @return the count of required indicator spans that have the "hidden" class
   */
  private int numberOfHiddenRequiredSpans(String renderedDiv) {
    Pattern hiddenRequiredPattern =
        Pattern.compile("class=\"usa-hint--required[^\"]*hidden[^\"]*\"");
    Matcher matcher = hiddenRequiredPattern.matcher(renderedDiv);
    int count = 0;
    while (matcher.find()) count++;
    return count;
  }

  @Before
  public void setup() {
    config = ConfigFactory.load();
    SettingsManifest settingsManifest = mock(SettingsManifest.class);
    AccountRepository mockAccountRepo = mock(AccountRepository.class);
    CategoryRepository mockCategoryRepo = mock(CategoryRepository.class);
    formBuilder =
        new ProgramFormBuilder(
            config,
            settingsManifest,
            mockAccountRepo,
            mockCategoryRepo,
            instanceOf(MessagesApi.class)) {};
  }

  @Test
  public void buildApplicationStepDiv_requiredStepOne() {
    DivTag stepOneRequiredDiv =
        formBuilder.buildApplicationStepDiv(0, ImmutableList.of(), /* isDisabled= */ false);
    String renderedDiv = stepOneRequiredDiv.render();

    assertThat(renderedDiv).contains("apply-step-1-title");
    assertThat(renderedDiv).contains("apply-step-1-description");

    assertThat(renderedDiv).contains("applicationSteps[0][title]");
    assertThat(renderedDiv).contains("applicationSteps[0][description]");

    assertThat(renderedDiv).contains("Step 1 title");
    assertThat(renderedDiv).contains("Step 1 description");
    assertThat(numberOfHiddenRequiredSpans(renderedDiv)).isEqualTo(0);
  }

  @Test
  public void buildApplicationStepDiv_optionalStepOne() {
    DivTag stepOneOptionalDiv =
        formBuilder.buildApplicationStepDiv(0, ImmutableList.of(), /* isDisabled= */ true);
    String renderedDiv = stepOneOptionalDiv.render();

    assertThat(renderedDiv).contains("Step 1 title");
    assertThat(renderedDiv).contains("Step 1 description");
    assertThat(numberOfHiddenRequiredSpans(renderedDiv)).isEqualTo(2);
  }

  @Test
  public void buildApplicationStepDiv_StepTwo() {
    // Application step #2 is always an optional field
    DivTag stepTwoDiv =
        formBuilder.buildApplicationStepDiv(1, ImmutableList.of(), /* isDisabled= */ false);
    String renderedDiv = stepTwoDiv.render();

    assertThat(renderedDiv).contains("Step 2 title");
    assertThat(renderedDiv).contains("Step 2 description");
    assertThat(numberOfHiddenRequiredSpans(renderedDiv)).isEqualTo(2);
  }

  @Test
  public void buildApplicationStepDiv_rendersExistingValues() {
    DivTag applicationStepDiv =
        formBuilder.buildApplicationStepDiv(
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
            " Create a program slug. This slug can only contain lowercase letters, numbers, and"
                + " dashes. It will be used in the program’s applicant-facing URL (except for"
                + " external programs), and it can’t be changed later.");
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
