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
import views.admin.programs.ProgramTranslationPageViewModel;

public final class ProgramTranslationPageMapperTest {

  private ProgramTranslationPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramTranslationPageMapper();
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
    Locale locale = Locale.FRENCH;
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramTranslationPageViewModel result =
        mapper.map(program, locale, statusDefinitions, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getProgramName()).isEqualTo("Test Program");
  }

  @Test
  public void map_setsCurrentLocaleDisplayName() {
    ProgramDefinition program = buildProgram();
    Locale locale = Locale.FRENCH;
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramTranslationPageViewModel result =
        mapper.map(program, locale, statusDefinitions, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getCurrentLocaleDisplayName()).isEqualTo("French");
  }

  @Test
  public void map_setsFormActionUrl() {
    ProgramDefinition program = buildProgram();
    Locale locale = Locale.FRENCH;
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramTranslationPageViewModel result =
        mapper.map(program, locale, statusDefinitions, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_setsLocaleLinks() {
    ProgramDefinition program = buildProgram();
    Locale french = Locale.FRENCH;
    Locale spanish = new Locale("es");
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramTranslationPageViewModel result =
        mapper.map(
            program,
            french,
            statusDefinitions,
            ImmutableList.of(french, spanish),
            Optional.empty());

    assertThat(result.getLocaleLinks()).hasSize(2);
  }

  @Test
  public void map_setsProgramDetailsSection() {
    ProgramDefinition program = buildProgram();
    Locale locale = Locale.FRENCH;
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramTranslationPageViewModel result =
        mapper.map(program, locale, statusDefinitions, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getSections()).isNotEmpty();
    assertThat(result.getSections().get(0).getLegend())
        .isEqualTo("Applicant-visible program details");
  }

  @Test
  public void map_withStatuses_includesStatusSections() {
    ProgramDefinition program = buildProgram();
    Locale locale = Locale.FRENCH;
    StatusDefinitions statusDefinitions =
        new StatusDefinitions(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("Approved")
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
                    .setLocalizedEmailBodyText(Optional.empty())
                    .setDefaultStatus(Optional.of(false))
                    .build()));

    ProgramTranslationPageViewModel result =
        mapper.map(program, locale, statusDefinitions, ImmutableList.of(locale), Optional.empty());

    // Should have program details section + status section
    assertThat(result.getSections().size()).isGreaterThanOrEqualTo(2);
    boolean hasStatusSection =
        result.getSections().stream()
            .anyMatch(s -> s.getLegend().contains("Application status: Approved"));
    assertThat(hasStatusSection).isTrue();
  }

  @Test
  public void map_setsErrorMessage() {
    ProgramDefinition program = buildProgram();
    Locale locale = Locale.FRENCH;
    StatusDefinitions statusDefinitions = new StatusDefinitions();

    ProgramTranslationPageViewModel result =
        mapper.map(
            program, locale, statusDefinitions, ImmutableList.of(locale), Optional.of("Error!"));

    assertThat(result.getErrorMessage()).contains("Error!");
  }
}
