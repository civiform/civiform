package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import models.LifecycleStage;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;

public class ProgramCardsSectionParamsFactoryTest extends ResetPostgres {

  @Test
  public void selectAndFormatDescription_usesShortDescriptionWhenPresent() {
    ProgramDefinition program = createProgramDefinition("short description", "long description");
    String description =
        ProgramCardsSectionParamsFactory.selectAndFormatDescription(program, Locale.getDefault());

    assertThat(description).isEqualTo("short description");
  }

  @Test
  public void selectAndFormatDescription_usesLongDescriptionWhenShortDescriptionIsBlank() {
    ProgramDefinition program = createProgramDefinition("", "long description");
    String description =
        ProgramCardsSectionParamsFactory.selectAndFormatDescription(program, Locale.getDefault());

    assertThat(description).isEqualTo("long description\n");
  }

  @Test
  public void selectAndFormatDescription_truncatesAndRemovesMarkdownFromLongDescription() {
    ProgramDefinition program =
        createProgramDefinition(
            "",
            "Here is a very long description with some markdown.\n"
                + "Here we have a [link](https://www.example.com) and some __bold text__.\n"
                + "Here we have a list:\n"
                + "- one\n"
                + "- two\n"
                + "- three\n"
                + "And some more text to make sure this is realllllllllyyyyyy long.");
    String description =
        ProgramCardsSectionParamsFactory.selectAndFormatDescription(program, Locale.getDefault());

    assertThat(description)
        .isEqualTo(
            "Here is a very long description with some markdown. Here we have a link and some bold"
                + " text. Here ...");
  }

  @Test
  public void getActionUrl_returnsProgramOverviewUrlWhenNotActiveOrDraft() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            1L,
            "fake-program",
            Optional
                .empty(), // empty lifecycle stage means this is their first time filling out this
                          // application
            Optional.empty(),
            Optional.empty());
    assertThat(url).isEqualTo("/programs/fake-program");
  }

  @Test
  public void getActionUrl_returnsEditUrlWhenDraft() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            1L,
            "fake-program",
            Optional.of(
                LifecycleStage.DRAFT), // draft lifecyle stage means they have an in progress draft
            Optional.empty(),
            Optional.empty());
    assertThat(url).isEqualTo("/programs/1/edit");
  }

  @Test
  public void getActionUrl_returnsReviewUrlWhenActive() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            1L,
            "fake-program",
            Optional.of(
                LifecycleStage
                    .ACTIVE), // active lifecycle stage means they have submitted the application
            Optional.empty(),
            Optional.empty());
    assertThat(url).isEqualTo("/programs/1/review");
  }

  private ProgramDefinition createProgramDefinition(
      String shortDescription, String longDescription) {
    return ProgramDefinition.builder()
        .setId(1L)
        .setAdminName("program-name")
        .setAdminDescription("admin description")
        .setLocalizedName(LocalizedStrings.withDefaultValue("program name"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue(longDescription))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue(shortDescription))
        .setExternalLink("https://www.example.com")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setApplicationSteps(ImmutableList.of())
        .build();
  }
}
