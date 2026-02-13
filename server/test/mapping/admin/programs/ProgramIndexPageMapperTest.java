package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import models.ProgramTab;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import views.admin.programs.ProgramIndexPageViewModel;

public final class ProgramIndexPageMapperTest {

  private ProgramIndexPageMapper mapper;
  private ProgramService mockProgramService;

  @Before
  public void setup() {
    mapper = new ProgramIndexPageMapper();
    mockProgramService = mock(ProgramService.class);
    when(mockProgramService.anyDisabledPrograms()).thenReturn(false);
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
  public void map_setsCivicEntityName() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenReturn(ImmutableSet.of());
    when(programs.getDraftPrograms()).thenReturn(ImmutableList.of());
    when(programs.anyDraft()).thenReturn(false);

    ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
    when(allPrograms.anyDraft()).thenReturn(false);
    when(allPrograms.getDraftPrograms()).thenReturn(ImmutableList.of());

    ActiveAndDraftQuestions questions = mock(ActiveAndDraftQuestions.class);

    ProgramIndexPageViewModel result =
        mapper.map(
            programs,
            questions,
            ProgramTab.IN_USE,
            "Test City",
            ImmutableList.of(),
            allPrograms,
            ImmutableList.of(),
            "http://localhost",
            mockProgramService,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getCivicEntityName()).isEqualTo("Test City");
  }

  @Test
  public void map_setsNewProgramUrl() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenReturn(ImmutableSet.of());
    when(programs.getDraftPrograms()).thenReturn(ImmutableList.of());
    when(programs.anyDraft()).thenReturn(false);

    ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
    when(allPrograms.anyDraft()).thenReturn(false);
    when(allPrograms.getDraftPrograms()).thenReturn(ImmutableList.of());

    ActiveAndDraftQuestions questions = mock(ActiveAndDraftQuestions.class);

    ProgramIndexPageViewModel result =
        mapper.map(
            programs,
            questions,
            ProgramTab.IN_USE,
            "Test City",
            ImmutableList.of(),
            allPrograms,
            ImmutableList.of(),
            "http://localhost",
            mockProgramService,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getNewProgramUrl()).isNotEmpty();
  }

  @Test
  public void map_inUseTab_setsTabSelection() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenReturn(ImmutableSet.of());
    when(programs.getDraftPrograms()).thenReturn(ImmutableList.of());
    when(programs.anyDraft()).thenReturn(false);

    ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
    when(allPrograms.anyDraft()).thenReturn(false);
    when(allPrograms.getDraftPrograms()).thenReturn(ImmutableList.of());

    ActiveAndDraftQuestions questions = mock(ActiveAndDraftQuestions.class);

    ProgramIndexPageViewModel result =
        mapper.map(
            programs,
            questions,
            ProgramTab.IN_USE,
            "Test City",
            ImmutableList.of(),
            allPrograms,
            ImmutableList.of(),
            "http://localhost",
            mockProgramService,
            Optional.empty(),
            Optional.empty());

    assertThat(result.isInUseTabSelected()).isTrue();
  }

  @Test
  public void map_withDraftPrograms_showsPublishAllButton() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenReturn(ImmutableSet.of());
    when(programs.getDraftPrograms()).thenReturn(ImmutableList.of());
    when(programs.anyDraft()).thenReturn(true);

    ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
    when(allPrograms.anyDraft()).thenReturn(true);
    when(allPrograms.getDraftPrograms()).thenReturn(ImmutableList.of(buildProgram(1L, "p", "P")));

    ActiveAndDraftQuestions questions = mock(ActiveAndDraftQuestions.class);

    ProgramIndexPageViewModel result =
        mapper.map(
            programs,
            questions,
            ProgramTab.IN_USE,
            "Test City",
            ImmutableList.of(),
            allPrograms,
            ImmutableList.of(),
            "http://localhost",
            mockProgramService,
            Optional.empty(),
            Optional.empty());

    assertThat(result.isShowPublishAllButton()).isTrue();
  }

  @Test
  public void map_setsSuccessAndErrorMessages() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenReturn(ImmutableSet.of());
    when(programs.getDraftPrograms()).thenReturn(ImmutableList.of());
    when(programs.anyDraft()).thenReturn(false);

    ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
    when(allPrograms.anyDraft()).thenReturn(false);
    when(allPrograms.getDraftPrograms()).thenReturn(ImmutableList.of());

    ActiveAndDraftQuestions questions = mock(ActiveAndDraftQuestions.class);

    ProgramIndexPageViewModel result =
        mapper.map(
            programs,
            questions,
            ProgramTab.IN_USE,
            "Test City",
            ImmutableList.of(),
            allPrograms,
            ImmutableList.of(),
            "http://localhost",
            mockProgramService,
            Optional.of("Published!"),
            Optional.of("Failed!"));

    assertThat(result.getSuccessMessage()).contains("Published!");
    assertThat(result.getErrorMessage()).contains("Failed!");
  }

  @Test
  public void map_setsDraftCounts() {
    ActiveAndDraftPrograms programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenReturn(ImmutableSet.of());
    when(programs.getDraftPrograms()).thenReturn(ImmutableList.of());
    when(programs.anyDraft()).thenReturn(false);

    ProgramDefinition draftProg = buildProgram(1L, "draft", "Draft");
    ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
    when(allPrograms.anyDraft()).thenReturn(true);
    when(allPrograms.getDraftPrograms()).thenReturn(ImmutableList.of(draftProg));

    var questions = mock(ActiveAndDraftQuestions.class);

    ProgramIndexPageViewModel result =
        mapper.map(
            programs,
            questions,
            ProgramTab.IN_USE,
            "Test City",
            ImmutableList.of(),
            allPrograms,
            ImmutableList.of(
                // TODO: ADD 3 items
                ),
            "http://localhost",
            mockProgramService,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getDraftProgramCount()).isEqualTo(1);
    assertThat(result.getDraftQuestionCount()).isEqualTo(3);
  }
}
