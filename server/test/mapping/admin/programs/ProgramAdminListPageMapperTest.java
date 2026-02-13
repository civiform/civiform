package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramAdminListPageViewModel;

public final class ProgramAdminListPageMapperTest {

  private ProgramAdminListPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramAdminListPageMapper();
  }

  private ProgramDefinition buildProgram(long id, String adminName, String displayName) {
    return ProgramDefinition.builder()
        .setId(id)
        .setAdminName(adminName)
        .setAdminDescription("desc")
        .setLocalizedName(LocalizedStrings.of(Locale.US, displayName))
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
  public void map_filtersToAuthorizedPrograms() {
    ProgramDefinition program1 = buildProgram(1L, "program-1", "Program 1");
    ProgramDefinition program2 = buildProgram(2L, "program-2", "Program 2");

    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getActivePrograms()).thenReturn(ImmutableList.of(program1, program2));

    ProgramAdminListPageViewModel result =
        mapper.map(programs, ImmutableList.of("program-1"), "http://localhost");

    assertThat(result.getPrograms()).hasSize(1);
    assertThat(result.getPrograms().get(0).getProgramName()).isEqualTo("Program 1");
  }

  @Test
  public void map_setsProgramCardFields() {
    ProgramDefinition program = buildProgram(1L, "test-program", "Test Program");

    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getActivePrograms()).thenReturn(ImmutableList.of(program));

    ProgramAdminListPageViewModel result =
        mapper.map(programs, ImmutableList.of("test-program"), "http://localhost");

    assertThat(result.getPrograms()).hasSize(1);
    ProgramAdminListPageViewModel.ProgramCardData card = result.getPrograms().get(0);
    assertThat(card.getProgramName()).isEqualTo("Test Program");
    assertThat(card.getAdminName()).isEqualTo("test-program");
    assertThat(card.getProgramId()).isEqualTo(1L);
    assertThat(card.isExternalProgram()).isFalse();
    assertThat(card.getShareLink()).isPresent();
  }

  @Test
  public void map_externalProgram_hasNoShareLink() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("ext-program")
            .setAdminDescription("desc")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "External"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Desc"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
            .setExternalLink("http://example.com")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.EXTERNAL)
            .setLocalizedConfirmationMessage(LocalizedStrings.empty())
            .setAcls(new ProgramAcls())
            .setBlockDefinitions(ImmutableList.of())
            .build();

    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getActivePrograms()).thenReturn(ImmutableList.of(program));

    ProgramAdminListPageViewModel result =
        mapper.map(programs, ImmutableList.of("ext-program"), "http://localhost");

    assertThat(result.getPrograms()).hasSize(1);
    assertThat(result.getPrograms().get(0).isExternalProgram()).isTrue();
    assertThat(result.getPrograms().get(0).getShareLink()).isEmpty();
  }

  @Test
  public void map_emptyPrograms_returnsEmptyList() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getActivePrograms()).thenReturn(ImmutableList.of());

    ProgramAdminListPageViewModel result =
        mapper.map(programs, ImmutableList.of(), "http://localhost");

    assertThat(result.getPrograms()).isEmpty();
  }
}
