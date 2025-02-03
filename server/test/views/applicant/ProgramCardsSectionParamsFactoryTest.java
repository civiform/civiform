package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import models.DisplayMode;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;

public class ProgramCardsSectionParamsFactoryTest extends ResetPostgres {

  @Test
  public void getCard_usesShortDescriptionWhenPresent() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("program-name")
            .setAdminDescription("admin description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("program name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("long description"))
            .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("short description"))
            .setExternalLink("https://www.example.com")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setApplicationSteps(ImmutableList.of())
            .build();

    String description =
        ProgramCardsSectionParamsFactory.selectAndFormatDescription(program, Locale.getDefault());
    assertThat(description).isEqualTo("short description");
  }

  @Test
  public void getCard_usesLongDescriptionWhenShortDescriptionIsBlank() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("program-name")
            .setAdminDescription("admin description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("program name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("long description"))
            .setLocalizedShortDescription(LocalizedStrings.withDefaultValue(""))
            .setExternalLink("https://www.example.com")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setApplicationSteps(ImmutableList.of())
            .build();

    String description =
        ProgramCardsSectionParamsFactory.selectAndFormatDescription(program, Locale.getDefault());
    assertThat(description).isEqualTo("long description\n");
  }

  @Test
  public void getCard_truncatesAndRemovesMarkdownFromLongDescription() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("program-name")
            .setAdminDescription("admin description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("program name"))
            .setLocalizedDescription(
                LocalizedStrings.withDefaultValue(
                    "Here is a very long description with some markdown.\n"
                        + "Here we have a [link](https://www.example.com) and some __bold text__.\n"
                        + "Here we have a list:\n"
                        + "- one\n"
                        + "- two\n"
                        + "- three\n"
                        + "And some more text to make sure this is realllllllllyyyyyy long."))
            .setLocalizedShortDescription(LocalizedStrings.withDefaultValue(""))
            .setExternalLink("https://www.example.com")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setApplicationSteps(ImmutableList.of())
            .build();

    String description =
        ProgramCardsSectionParamsFactory.selectAndFormatDescription(program, Locale.getDefault());
    assertThat(description)
        .isEqualTo(
            "Here is a very long description with some markdown. Here we have a link and some bold"
                + " text. Here ...");
  }
}
