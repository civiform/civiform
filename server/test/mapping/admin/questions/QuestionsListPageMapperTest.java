package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.DeletionStatus;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.question.ActiveAndDraftQuestions;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionsListPageViewModel;

public final class QuestionsListPageMapperTest {

  private QuestionsListPageMapper mapper;
  private DateConverter mockDateConverter;
  private QuestionService mockQuestionService;
  private TranslationLocales mockTranslationLocales;
  private ActiveAndDraftQuestions mockActiveAndDraftQuestions;

  @Before
  public void setup() {
    mapper = new QuestionsListPageMapper();
    mockDateConverter = mock(DateConverter.class);
    mockQuestionService = mock(QuestionService.class);
    mockTranslationLocales = mock(TranslationLocales.class);
    mockActiveAndDraftQuestions = mock(ActiveAndDraftQuestions.class);
    when(mockDateConverter.renderDateTimeHumanReadable(any(Instant.class)))
        .thenReturn("Jan 1, 2024");
  }

  @Test
  public void map_emptyQuestions_returnsEmptyLists() {
    when(mockActiveAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of());

    QuestionsListPageViewModel result =
        mapper.map(
            mockActiveAndDraftQuestions,
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            mockQuestionService,
            mockTranslationLocales,
            false,
            false,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getTotalQuestionCount()).isEqualTo(0);
    assertThat(result.getUniversalQuestions()).isEmpty();
    assertThat(result.getNonUniversalQuestions()).isEmpty();
    assertThat(result.getArchivedQuestions()).isEmpty();
  }

  @Test
  public void map_setsCreateQuestionUrl() {
    when(mockActiveAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of());

    QuestionsListPageViewModel result =
        mapper.map(
            mockActiveAndDraftQuestions,
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            mockQuestionService,
            mockTranslationLocales,
            false,
            false,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getCreateQuestionUrl()).isNotEmpty();
  }

  @Test
  public void map_setsFilter() {
    when(mockActiveAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of());

    QuestionsListPageViewModel result =
        mapper.map(
            mockActiveAndDraftQuestions,
            Optional.of("search-term"),
            false,
            false,
            mockDateConverter,
            mockQuestionService,
            mockTranslationLocales,
            false,
            false,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getInitialFilter()).contains("search-term");
  }

  @Test
  public void map_withQuestion_categorizesByUniversal() {
    String questionName = "test-question";
    QuestionDefinition draft = mock(QuestionDefinition.class);
    when(draft.getName()).thenReturn(questionName);
    when(draft.getId()).thenReturn(1L);
    when(draft.getQuestionText()).thenReturn(LocalizedStrings.withDefaultValue("What?"));
    when(draft.getQuestionHelpText()).thenReturn(LocalizedStrings.empty());
    when(draft.getQuestionType()).thenReturn(QuestionType.TEXT);
    when(draft.getDescription()).thenReturn("desc");
    when(draft.isUniversal()).thenReturn(true);
    when(draft.getLastModifiedTime()).thenReturn(Optional.of(Instant.now()));

    when(mockActiveAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of(questionName));
    when(mockActiveAndDraftQuestions.getDraftQuestionDefinition(questionName))
        .thenReturn(Optional.of(draft));
    when(mockActiveAndDraftQuestions.getActiveQuestionDefinition(questionName))
        .thenReturn(Optional.empty());
    when(mockActiveAndDraftQuestions.getReferencingPrograms(questionName))
        .thenReturn(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                .setDraftReferences(ImmutableSet.of())
                .build());
    when(mockActiveAndDraftQuestions.getDeletionStatus(questionName))
        .thenReturn(DeletionStatus.DELETABLE);

    QuestionsListPageViewModel result =
        mapper.map(
            mockActiveAndDraftQuestions,
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            mockQuestionService,
            mockTranslationLocales,
            false,
            false,
            Optional.empty(),
            Optional.empty());

    assertThat(result.getTotalQuestionCount()).isEqualTo(1);
    assertThat(result.getUniversalQuestions()).hasSize(1);
    assertThat(result.getNonUniversalQuestions()).isEmpty();
  }

  @Test
  public void map_setsSuccessAndErrorMessages() {
    when(mockActiveAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of());

    QuestionsListPageViewModel result =
        mapper.map(
            mockActiveAndDraftQuestions,
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            mockQuestionService,
            mockTranslationLocales,
            false,
            false,
            Optional.of("Created!"),
            Optional.of("Failed!"));

    assertThat(result.getSuccessMessage()).contains("Created!");
    assertThat(result.getErrorMessage()).contains("Failed!");
  }

  @Test
  public void map_setsTranslationManagementEnabled() {
    when(mockActiveAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of());

    QuestionsListPageViewModel result =
        mapper.map(
            mockActiveAndDraftQuestions,
            Optional.empty(),
            true,
            false,
            mockDateConverter,
            mockQuestionService,
            mockTranslationLocales,
            false,
            false,
            Optional.empty(),
            Optional.empty());

    assertThat(result.isTranslationManagementEnabled()).isTrue();
  }
}
