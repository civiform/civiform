package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Optional;
import models.ApplicationStep;
import models.DisplayMode;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.migration.AdminImportProgramDataPartialViewModel;
import views.admin.migration.AdminImportProgramDataPartialViewModel.Block;
import views.admin.migration.AdminImportProgramDataPartialViewModel.QuestionCard;

public final class AdminImportProgramDataPartialMapperTest {

  private static final String PROGRAM_JSON = "{\"program\": {}}";

  private final AdminImportProgramDataPartialMapper mapper =
      new AdminImportProgramDataPartialMapper();

  private static QuestionDefinition mockQuestion(String name, long id) {
    QuestionDefinition question = mock(QuestionDefinition.class);
    when(question.getName()).thenReturn(name);
    when(question.getId()).thenReturn(id);
    when(question.getQuestionType()).thenReturn(QuestionType.TEXT);
    when(question.getQuestionText()).thenReturn(LocalizedStrings.withDefaultValue(name + " text"));
    when(question.getQuestionHelpText())
        .thenReturn(LocalizedStrings.withDefaultValue(name + " help"));
    when(question.getEnumeratorId()).thenReturn(Optional.empty());
    when(question.isUniversal()).thenReturn(false);
    when(question.isEnumerator()).thenReturn(false);
    when(question.isRepeated()).thenReturn(false);
    return question;
  }

  private static BlockDefinition block(long id, QuestionDefinition... questions) {
    return BlockDefinition.builder()
        .setId(id)
        .setName("Screen " + id)
        .setDescription("Screen " + id + " description")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Screen " + id))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen " + id + " description"))
        .setProgramQuestionDefinitions(
            Arrays.stream(questions)
                .map(
                    question ->
                        ProgramQuestionDefinition.create(
                            question, /* programDefinitionId= */ Optional.empty()))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private static ProgramDefinition program(BlockDefinition... blocks) {
    ProgramDefinition.Builder builder =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("program-admin-name")
            .setAdminDescription("admin description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Program Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
            .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("short description"))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setLoginOnly(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
            .setBridgeDefinitions(ImmutableMap.of());

    for (BlockDefinition blockDefinition : blocks) {
      builder.addBlockDefinition(blockDefinition);
    }

    return builder.build();
  }

  @Test
  public void map_setsProgramTitleAndAdminName() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    assertThat(result.getProgramTitle()).isEqualTo("Program Name");
    assertThat(result.getAdminName()).isEqualTo("program-admin-name");
  }

  @Test
  public void map_setsUrlsAndProgramJson() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    assertThat(result.getHxSaveProgramUrl()).isEqualTo("/admin/import/hx/saveProgram");
    assertThat(result.getStartOverUrl()).isEqualTo("/admin/import");
    assertThat(result.getProgramJson()).isEqualTo(PROGRAM_JSON);
  }

  @Test
  public void map_singleNewQuestion_setsInfoAlert() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    assertThat(result.isShowQuestionAlert()).isTrue();
    assertThat(result.getQuestionAlertTypeClass()).isEqualTo("usa-alert--info");
    assertThat(result.getQuestionAlertText())
        .isEqualTo("This program will add 1 new question to the question bank.");
  }

  @Test
  public void map_newAndDuplicateQuestions_setsWarningAlertWithBothCounts() {
    QuestionDefinition question1 = mockQuestion("q1", 10L);
    QuestionDefinition question2 = mockQuestion("q2", 11L);
    QuestionDefinition question3 = mockQuestion("q3", 12L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question1, question2, question3)),
            ImmutableList.of(question1, question2, question3),
            /* duplicateQuestionNames= */ ImmutableList.of("q2", "q3"),
            PROGRAM_JSON);

    assertThat(result.getQuestionAlertTypeClass()).isEqualTo("usa-alert--warning");
    assertThat(result.getQuestionAlertText())
        .isEqualTo(
            "This program will add 1 new question to the question bank and contains 2 duplicate"
                + " questions that must be resolved.");
  }

  @Test
  public void map_duplicatesOnly_setsWarningAlert() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of("q1"),
            PROGRAM_JSON);

    assertThat(result.getQuestionAlertTypeClass()).isEqualTo("usa-alert--warning");
    assertThat(result.getQuestionAlertText())
        .isEqualTo("This program contains 1 duplicate question that must be resolved.");
  }

  @Test
  public void map_noQuestions_hidesQuestionAlert() {
    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L)),
            /* questions= */ ImmutableList.of(),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    assertThat(result.isShowQuestionAlert()).isFalse();
  }

  @Test
  public void map_nullQuestions_throwsLikeTheLegacyView() {
    // The legacy view dereferenced a null questions list; the controller's
    // catch block turns the exception into the error partial. Keep parity.
    assertThatThrownBy(
            () ->
                mapper.map(
                    program(block(1L)),
                    /* questions= */ null,
                    /* duplicateQuestionNames= */ ImmutableList.of(),
                    PROGRAM_JSON))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void map_showsToplevelDuplicateOptionsOnlyWithMultipleDuplicates() {
    QuestionDefinition question1 = mockQuestion("q1", 10L);
    QuestionDefinition question2 = mockQuestion("q2", 11L);

    AdminImportProgramDataPartialViewModel oneDuplicate =
        mapper.map(
            program(block(1L, question1, question2)),
            ImmutableList.of(question1, question2),
            /* duplicateQuestionNames= */ ImmutableList.of("q1"),
            PROGRAM_JSON);
    AdminImportProgramDataPartialViewModel twoDuplicates =
        mapper.map(
            program(block(1L, question1, question2)),
            ImmutableList.of(question1, question2),
            /* duplicateQuestionNames= */ ImmutableList.of("q1", "q2"),
            PROGRAM_JSON);

    assertThat(oneDuplicate.isShowToplevelDuplicateOptions()).isFalse();
    assertThat(twoDuplicates.isShowToplevelDuplicateOptions()).isTrue();
  }

  @Test
  public void map_buildsBlockWithNewQuestionCard() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    assertThat(result.getBlocks()).hasSize(1);
    Block resultBlock = result.getBlocks().get(0);
    assertThat(resultBlock.getName()).isEqualTo("Screen 1");
    assertThat(resultBlock.getDescription()).isEqualTo("Screen 1 description");

    assertThat(resultBlock.getQuestionCards()).hasSize(1);
    QuestionCard card = resultBlock.getQuestionCards().get(0);
    assertThat(card.getAdminName()).isEqualTo("q1");
    assertThat(card.getQuestionTextHtml()).contains("q1 text");
    assertThat(card.getHelpTextHtml()).contains("q1 help");
    assertThat(card.getIconFragment()).isEqualTo("iconText");
    assertThat(card.isDuplicate()).isFalse();
    assertThat(card.getUniversalBadgeText()).isNull();
    assertThat(card.getEnumeratorName()).isNull();
    assertThat(card.getOptionTexts()).isNull();
    assertThat(card.getDuplicateHandling()).isNull();
  }

  @Test
  public void map_blockWithoutQuestionsInJson_hasNoQuestionCards() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    // The block references a question, but the JSON contained no questions;
    // the legacy view only rendered cards when the questions map was
    // populated.
    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            /* questions= */ ImmutableList.of(),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    assertThat(result.getBlocks().get(0).getQuestionCards()).isEmpty();
  }

  @Test
  public void map_duplicateQuestion_setsBadgeAndHandlingOptions() {
    QuestionDefinition question = mockQuestion("q1", 10L);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of("q1"),
            PROGRAM_JSON);

    QuestionCard card = result.getBlocks().get(0).getQuestionCards().get(0);
    assertThat(card.isDuplicate()).isTrue();
    assertThat(card.getDuplicateHandling()).isNotNull();
    assertThat(card.getDuplicateHandling().getAdminName()).isEqualTo("q1");
    assertThat(card.getDuplicateHandling().isEnumerator()).isFalse();
    assertThat(card.getDuplicateHandling().isRepeated()).isFalse();
    assertThat(card.getDuplicateHandling().getExistingQuestionUrl())
        .isEqualTo(
            controllers.admin.routes.AdminQuestionController.index(Optional.of("Admin ID: q1"))
                .url());
  }

  @Test
  public void map_repeatedQuestion_setsEnumeratorName() {
    QuestionDefinition enumerator = mockQuestion("enumerator-q", 10L);
    QuestionDefinition repeated = mockQuestion("repeated-q", 11L);
    when(repeated.getEnumeratorId()).thenReturn(Optional.of(10L));

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, enumerator, repeated)),
            ImmutableList.of(enumerator, repeated),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    ImmutableList<QuestionCard> cards = result.getBlocks().get(0).getQuestionCards();
    assertThat(cards.get(0).getEnumeratorName()).isNull();
    assertThat(cards.get(1).getEnumeratorName()).isEqualTo("enumerator-q");
  }

  @Test
  public void map_universalQuestion_setsBadgeText() {
    QuestionDefinition question = mockQuestion("q1", 10L);
    when(question.isUniversal()).thenReturn(true);

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    QuestionCard card = result.getBlocks().get(0).getQuestionCards().get(0);
    assertThat(card.getUniversalBadgeText()).isEqualTo("Universal text question");
  }

  @Test
  public void map_multiOptionQuestion_setsOptionTexts() {
    MultiOptionQuestionDefinition question = mock(MultiOptionQuestionDefinition.class);
    when(question.getName()).thenReturn("q1");
    when(question.getId()).thenReturn(10L);
    when(question.getQuestionType()).thenReturn(QuestionType.CHECKBOX);
    when(question.getQuestionText()).thenReturn(LocalizedStrings.withDefaultValue("q1 text"));
    when(question.getQuestionHelpText()).thenReturn(LocalizedStrings.empty());
    when(question.getEnumeratorId()).thenReturn(Optional.empty());
    when(question.isUniversal()).thenReturn(false);
    when(question.getOptions())
        .thenReturn(
            ImmutableList.of(
                QuestionOption.create(
                    1L,
                    /* displayOrder= */ 1L,
                    "option-one",
                    LocalizedStrings.withDefaultValue("Option one")),
                QuestionOption.create(
                    2L,
                    /* displayOrder= */ 2L,
                    "option-two",
                    LocalizedStrings.withDefaultValue("Option two"))));

    AdminImportProgramDataPartialViewModel result =
        mapper.map(
            program(block(1L, question)),
            ImmutableList.of(question),
            /* duplicateQuestionNames= */ ImmutableList.of(),
            PROGRAM_JSON);

    QuestionCard card = result.getBlocks().get(0).getQuestionCards().get(0);
    assertThat(card.getIconFragment()).isEqualTo("iconCheckbox");
    assertThat(card.getOptionTexts()).containsExactly("Option one", "Option two");
  }
}
