package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import repository.VersionRepository.PublishProgramPreview;
import services.DateConverter;
import services.DeletionStatus;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionsListPageViewModel;
import views.admin.questions.QuestionsListPageViewModel.ExtraAction;
import views.admin.questions.QuestionsListPageViewModel.ModalModel;
import views.admin.questions.QuestionsListPageViewModel.QuestionRow;

// ActiveAndDraftQuestions.ReferencingPrograms is an AutoValue whose builder is package-private in
// services.question, so it can't be constructed from this package; mock it instead.
@SuppressWarnings("DoNotMockAutoValue")
public final class QuestionsListPageMapperTest {

  private QuestionsListPageMapper mapper;
  private ActiveAndDraftQuestions activeAndDraftQuestions;
  private TranslationLocales translationLocales;
  private Set<QuestionDefinition> questionsWithCompleteTranslations;
  private DateConverter dateConverter;

  @Before
  public void setup() {
    mapper = new QuestionsListPageMapper();
    questionsWithCompleteTranslations = new HashSet<>();
    activeAndDraftQuestions = mock(ActiveAndDraftQuestions.class);
    translationLocales = mock(TranslationLocales.class);
    dateConverter = mock(DateConverter.class);

    when(activeAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of());
    when(translationLocales.translatableLocales()).thenReturn(ImmutableList.of());
  }

  private QuestionsListPageViewModel map() {
    return map(/* filter= */ Optional.empty());
  }

  private QuestionsListPageViewModel map(Optional<String> filter) {
    return mapper.map(
        activeAndDraftQuestions,
        filter,
        translationLocales,
        questionsWithCompleteTranslations::contains,
        dateConverter,
        /* enumeratorImprovementsEnabled= */ false,
        /* successMessage= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  private QuestionDefinition mockQuestion(String name, long id) {
    QuestionDefinition question = mock(QuestionDefinition.class);
    when(question.getName()).thenReturn(name);
    when(question.getId()).thenReturn(id);
    when(question.getQuestionType()).thenReturn(QuestionType.TEXT);
    when(question.getQuestionText()).thenReturn(LocalizedStrings.withDefaultValue(name + " text"));
    when(question.getQuestionHelpText()).thenReturn(LocalizedStrings.empty());
    when(question.getDescription()).thenReturn(name + " description");
    when(question.getLastModifiedTime()).thenReturn(Optional.of(Instant.EPOCH));
    when(question.isUniversal()).thenReturn(false);
    return question;
  }

  private void addQuestion(
      String name, Optional<QuestionDefinition> active, Optional<QuestionDefinition> draft) {
    when(activeAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of(name));
    when(activeAndDraftQuestions.getActiveQuestionDefinition(name)).thenReturn(active);
    when(activeAndDraftQuestions.getDraftQuestionDefinition(name)).thenReturn(draft);
    when(activeAndDraftQuestions.getDeletionStatus(name)).thenReturn(DeletionStatus.DELETABLE);
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        mock(ActiveAndDraftQuestions.ReferencingPrograms.class);
    when(referencingPrograms.activeReferences()).thenReturn(ImmutableSet.of());
    when(referencingPrograms.draftReferences()).thenReturn(ImmutableSet.of());
    when(activeAndDraftQuestions.getReferencingPrograms(name)).thenReturn(referencingPrograms);
  }

  private PublishProgramPreview programPreview(String adminName) {
    return new PublishProgramPreview(
        adminName, DisplayMode.PUBLIC, LocalizedStrings.withDefaultValue(adminName + " display"));
  }

  @Test
  public void map_setsTotalQuestionCount() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    assertThat(result.getTotalQuestionCount()).isEqualTo(1);
  }

  @Test
  public void map_setsFilterValue() {
    QuestionsListPageViewModel result = map(Optional.of("name"));

    assertThat(result.getFilterValue()).isEqualTo("name");
  }

  @Test
  public void map_emptyFilter_setsEmptyFilterValue() {
    QuestionsListPageViewModel result = map();

    assertThat(result.getFilterValue()).isEmpty();
  }

  @Test
  public void map_buildsSortOptionsWithLastModifiedSelectedFirst() {
    QuestionsListPageViewModel result = map();

    assertThat(result.getSortOptions()).hasSize(5);
    assertThat(result.getSortOptions().get(0).getValue()).isEqualTo("lastmodified-desc");
    assertThat(result.getSortOptions().get(0).getLabel()).isEqualTo("Last modified");
    assertThat(result.getSortOptions().get(0).isSelected()).isTrue();
    assertThat(result.getSortOptions().get(1).isSelected()).isFalse();
  }

  @Test
  public void map_buildsCreateQuestionOptionsForAllTypesExceptNullQuestion() {
    QuestionsListPageViewModel result = map();

    assertThat(result.getCreateQuestionOptions()).hasSize(QuestionType.values().length - 1);
    assertThat(result.getCreateQuestionOptions().get(0).getId())
        .isEqualTo("create-address-question");
    assertThat(result.getCreateQuestionOptions().get(0).getUrl()).contains("/admin/questions/new");
    assertThat(result.getCreateQuestionOptions().get(0).getIconFragment()).isEqualTo("iconAddress");
    assertThat(result.getCreateQuestionOptions().get(0).getSvgLinkId())
        .isEqualTo("svg-link-address");
  }

  @Test
  public void map_createQuestionOptions_remappedTypesUseLegacyIconNames() {
    QuestionsListPageViewModel result = map();

    // STATIC renders the ANNOTATION icon and YES_NO the RADIO_BUTTON icon,
    // mirroring the legacy Icons.getIconTypeFromQuestionType mapping.
    assertThat(result.getCreateQuestionOptions())
        .filteredOn(option -> option.getId().equals("create-static-question"))
        .allSatisfy(
            option -> {
              assertThat(option.getIconFragment()).isEqualTo("iconAnnotation");
              assertThat(option.getSvgLinkId()).isEqualTo("svg-link-annotation");
            })
        .isNotEmpty();
    assertThat(result.getCreateQuestionOptions())
        .filteredOn(option -> option.getId().equals("create-yes_no-question"))
        .allSatisfy(
            option -> {
              assertThat(option.getIconFragment()).isEqualTo("iconRadioButton");
              assertThat(option.getSvgLinkId()).isEqualTo("svg-link-radio_button");
            })
        .isNotEmpty();
  }

  @Test
  public void map_questionRow_mapsQuestionTypeToIconFragment() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    // mockQuestion builds a TEXT question.
    assertThat(result.getOtherRows().get(0).getIconFragment()).isEqualTo("iconText");
  }

  @Test
  public void map_enumeratorImprovementsEnabled_hidesEnumeratorOption() {
    QuestionsListPageViewModel result =
        mapper.map(
            activeAndDraftQuestions,
            /* filter= */ Optional.empty(),
            translationLocales,
            questionsWithCompleteTranslations::contains,
            dateConverter,
            /* enumeratorImprovementsEnabled= */ true,
            /* successMessage= */ Optional.empty(),
            /* errorMessage= */ Optional.empty());

    assertThat(result.getCreateQuestionOptions())
        .noneMatch(option -> option.getId().equals("create-enumerator-question"));
  }

  @Test
  public void map_successMessage_buildsDismissibleSuccessToast() {
    QuestionsListPageViewModel result =
        mapper.map(
            activeAndDraftQuestions,
            /* filter= */ Optional.empty(),
            translationLocales,
            questionsWithCompleteTranslations::contains,
            dateConverter,
            /* enumeratorImprovementsEnabled= */ false,
            /* successMessage= */ Optional.of("question created"),
            /* errorMessage= */ Optional.empty());

    assertThat(result.getToasts()).hasSize(1);
    assertThat(result.getToasts().get(0).getMessage()).isEqualTo("question created");
    assertThat(result.getToasts().get(0).getType()).isEqualTo("SUCCESS");
    assertThat(result.getToasts().get(0).isCanDismiss()).isTrue();
  }

  @Test
  public void map_errorMessage_buildsNonDismissibleErrorToastWithPrefix() {
    QuestionsListPageViewModel result =
        mapper.map(
            activeAndDraftQuestions,
            /* filter= */ Optional.empty(),
            translationLocales,
            questionsWithCompleteTranslations::contains,
            dateConverter,
            /* enumeratorImprovementsEnabled= */ false,
            /* successMessage= */ Optional.empty(),
            /* errorMessage= */ Optional.of("something broke"));

    assertThat(result.getToasts()).hasSize(1);
    assertThat(result.getToasts().get(0).getMessage()).isEqualTo("Error: something broke");
    assertThat(result.getToasts().get(0).getType()).isEqualTo("ERROR");
    assertThat(result.getToasts().get(0).isCanDismiss()).isFalse();
  }

  @Test
  public void map_activeOnlyQuestion_buildsActiveRowWithVisibleEdit() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    assertThat(result.getOtherRows()).hasSize(1);
    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getDraftRow()).isNull();
    assertThat(row.getActiveRow()).isNotNull();
    assertThat(row.getActiveRow().getBadgeText()).isEqualTo("Active");
    assertThat(row.getActiveRow().isSecondRow()).isFalse();
    assertThat(row.getActiveRow().isEditVisible()).isTrue();
    assertThat(row.getActiveRow().getEditUrl()).startsWith("/admin/questions/1/edit");
  }

  @Test
  public void map_draftAndActive_editOnlyOnDraftRow() {
    QuestionDefinition active = mockQuestion("q1", 1L);
    QuestionDefinition draft = mockQuestion("q1", 2L);
    addQuestion("q1", Optional.of(active), Optional.of(draft));

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getDraftRow().isEditVisible()).isTrue();
    assertThat(row.getDraftRow().isSecondRow()).isFalse();
    assertThat(row.getActiveRow().isEditVisible()).isFalse();
    assertThat(row.getActiveRow().isSecondRow()).isTrue();
  }

  @Test
  public void map_draftWithActive_addsDiscardActionAndModal() {
    QuestionDefinition active = mockQuestion("q1", 1L);
    QuestionDefinition draft = mockQuestion("q1", 2L);
    addQuestion("q1", Optional.of(active), Optional.of(draft));

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getDraftRow().getExtraActions())
        .anyMatch(action -> action.getKind() == ExtraAction.Kind.DISCARD);
    assertThat(result.getModals())
        .anyMatch(
            modal ->
                modal.getType() == ModalModel.Type.DISCARD_DRAFT
                    && modal.getId().equals("discard-confirmation-modal")
                    && modal.getDiscardUrl().equals("/admin/questions/2/discard"));
  }

  @Test
  public void map_draftWithoutTranslatableLocales_hasNoTranslateAction() {
    QuestionDefinition draft = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.empty(), Optional.of(draft));

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getDraftRow().getExtraActions())
        .noneMatch(action -> action.getKind() == ExtraAction.Kind.TRANSLATE);
  }

  @Test
  public void map_draftWithTranslatableLocales_addsTranslateAction() {
    when(translationLocales.translatableLocales()).thenReturn(ImmutableList.of(Locale.GERMAN));
    QuestionDefinition draft = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.empty(), Optional.of(draft));

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getDraftRow().getExtraActions())
        .anyMatch(
            action ->
                action.getKind() == ExtraAction.Kind.TRANSLATE
                    && action.getUrl().equals("/admin/questions/q1/translations/edit"));
  }

  @Test
  public void map_deletableQuestion_addsArchiveAction() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getActiveRow().getExtraActions())
        .anyMatch(
            action ->
                action.getKind() == ExtraAction.Kind.ARCHIVE
                    && action.getUrl().equals("/admin/questions/1/archive")
                    && !action.getFormId().isEmpty());
  }

  @Test
  public void map_pendingDeletionQuestion_groupedIntoArchivedRowsWithRestoreAction() {
    QuestionDefinition draft = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.empty(), Optional.of(draft));
    when(activeAndDraftQuestions.getDeletionStatus("q1"))
        .thenReturn(DeletionStatus.PENDING_DELETION);

    QuestionsListPageViewModel result = map();

    assertThat(result.getOtherRows()).isEmpty();
    assertThat(result.getArchivedRows()).hasSize(1);
    QuestionRow row = result.getArchivedRows().get(0);
    assertThat(row.getDraftRow().getBadgeText()).isEqualTo("Archived");
    assertThat(row.getDraftRow().getExtraActions())
        .anyMatch(
            action ->
                action.getKind() == ExtraAction.Kind.RESTORE
                    && action.getUrl().equals("/admin/questions/1/restore"));
  }

  @Test
  public void map_notDeletableQuestion_addsArchiveBlockedActionWithCantArchiveModal() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());
    when(activeAndDraftQuestions.getDeletionStatus("q1")).thenReturn(DeletionStatus.NOT_DELETABLE);
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        mock(ActiveAndDraftQuestions.ReferencingPrograms.class);
    when(referencingPrograms.activeReferences()).thenReturn(ImmutableSet.of());
    when(referencingPrograms.draftReferences())
        .thenReturn(ImmutableSet.of(programPreview("program-a")));
    when(activeAndDraftQuestions.getReferencingPrograms("q1")).thenReturn(referencingPrograms);

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    Optional<ExtraAction> blocked =
        row.getActiveRow().getExtraActions().stream()
            .filter(action -> action.getKind() == ExtraAction.Kind.ARCHIVE_BLOCKED)
            .findFirst();
    assertThat(blocked).isPresent();
    // Two referencing modals: the "See list" one and the can't-archive one.
    assertThat(result.getModals()).hasSize(2);
    ModalModel cantArchiveModal = result.getModals().get(1);
    assertThat(cantArchiveModal.isShowCantArchiveHeader()).isTrue();
    assertThat(blocked.get().getTriggerButtonId()).isEqualTo(cantArchiveModal.getId() + "-button");
  }

  @Test
  public void map_universalQuestion_groupedIntoUniversalRowsWithBadge() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    when(question.isUniversal()).thenReturn(true);
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    assertThat(result.getOtherRows()).isEmpty();
    assertThat(result.getUniversalRows()).hasSize(1);
    assertThat(result.getUniversalRows().get(0).getUniversalBadgeText())
        .isEqualTo("Universal text question");
  }

  @Test
  public void map_noReferencingPrograms_usedInZeroProgramsAndNoSeeList() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getReferencingLines()).containsExactly("Used in 0 programs.");
    assertThat(row.getSeeListTriggerId()).isNull();
    assertThat(row.getNumReferencingPrograms()).isZero();
  }

  @Test
  public void map_addedPrograms_buildsReferencingLineAndModal() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        mock(ActiveAndDraftQuestions.ReferencingPrograms.class);
    when(referencingPrograms.activeReferences()).thenReturn(ImmutableSet.of());
    when(referencingPrograms.draftReferences())
        .thenReturn(ImmutableSet.of(programPreview("program-a"), programPreview("program-b")));
    when(activeAndDraftQuestions.getReferencingPrograms("q1")).thenReturn(referencingPrograms);

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getReferencingLines()).containsExactly("Added to 2 programs in use.");
    assertThat(row.getNumReferencingPrograms()).isEqualTo(2);
    assertThat(result.getModals()).hasSize(1);
    ModalModel modal = result.getModals().get(0);
    assertThat(modal.getType()).isEqualTo(ModalModel.Type.REFERENCING_PROGRAMS);
    assertThat(modal.getTitle()).isEqualTo("Programs referencing q1");
    assertThat(modal.getAddedProgramNames())
        .containsExactly("program-a display", "program-b display");
    assertThat(row.getSeeListTriggerId()).isEqualTo(modal.getId() + "-button");
  }

  @Test
  public void map_disabledDraftPrograms_buildsDisabledLineOnly() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        mock(ActiveAndDraftQuestions.ReferencingPrograms.class);
    when(referencingPrograms.activeReferences()).thenReturn(ImmutableSet.of());
    when(referencingPrograms.draftReferences())
        .thenReturn(
            ImmutableSet.of(
                new PublishProgramPreview(
                    "program-a",
                    DisplayMode.DISABLED,
                    LocalizedStrings.withDefaultValue("program-a display"))));
    when(activeAndDraftQuestions.getReferencingPrograms("q1")).thenReturn(referencingPrograms);

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getReferencingLines()).containsExactly("Added to  1 disabled program.");
    assertThat(row.getNumReferencingPrograms()).isZero();
  }

  @Test
  public void map_sortsUniversalAndNewestFirst() {
    QuestionDefinition older = mockQuestion("older", 1L);
    when(older.getLastModifiedTime()).thenReturn(Optional.of(Instant.ofEpochSecond(100)));
    QuestionDefinition newer = mockQuestion("newer", 2L);
    when(newer.getLastModifiedTime()).thenReturn(Optional.of(Instant.ofEpochSecond(200)));

    when(activeAndDraftQuestions.getQuestionNames()).thenReturn(ImmutableSet.of("older", "newer"));
    for (QuestionDefinition question : ImmutableList.of(older, newer)) {
      String name = question.getName();
      when(activeAndDraftQuestions.getActiveQuestionDefinition(name))
          .thenReturn(Optional.of(question));
      when(activeAndDraftQuestions.getDraftQuestionDefinition(name)).thenReturn(Optional.empty());
      when(activeAndDraftQuestions.getDeletionStatus(name)).thenReturn(DeletionStatus.DELETABLE);
      ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
          mock(ActiveAndDraftQuestions.ReferencingPrograms.class);
      when(referencingPrograms.activeReferences()).thenReturn(ImmutableSet.of());
      when(referencingPrograms.draftReferences()).thenReturn(ImmutableSet.of());
      when(activeAndDraftQuestions.getReferencingPrograms(name)).thenReturn(referencingPrograms);
    }

    QuestionsListPageViewModel result = map();

    assertThat(result.getOtherRows()).hasSize(2);
    assertThat(result.getOtherRows().get(0).getAdminName()).isEqualTo("newer");
    assertThat(result.getOtherRows().get(1).getAdminName()).isEqualTo("older");
  }

  @Test
  public void map_setsEditedOnTextsFromDateConverter() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());
    when(dateConverter.renderDateTimeHumanReadable(Instant.EPOCH)).thenReturn("time text");
    when(dateConverter.renderDate(Instant.EPOCH)).thenReturn("date text");

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getActiveRow().getEditedOnTimeText()).isEqualTo("time text");
    assertThat(row.getActiveRow().getEditedOnDateText()).isEqualTo("date text");
  }

  @Test
  public void map_missingLastModified_rendersUnknown() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    when(question.getLastModifiedTime()).thenReturn(Optional.empty());
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getActiveRow().getEditedOnTimeText()).isEqualTo("unknown");
    assertThat(row.getActiveRow().getEditedOnDateText()).isEqualTo("unknown");
    assertThat(row.getLastModified()).isEqualTo(Instant.EPOCH.toString());
  }

  @Test
  public void map_formatsQuestionTextAsHtml() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    when(question.getQuestionText())
        .thenReturn(LocalizedStrings.withDefaultValue("some **bold** text"));
    addQuestion("q1", Optional.of(question), Optional.empty());

    QuestionsListPageViewModel result = map();

    assertThat(result.getOtherRows().get(0).getQuestionTextHtml())
        .contains("<strong>bold</strong>");
  }

  @Test
  public void map_setsTranslationCompleteFromChecker() {
    QuestionDefinition question = mockQuestion("q1", 1L);
    addQuestion("q1", Optional.of(question), Optional.empty());
    questionsWithCompleteTranslations.add(question);

    QuestionsListPageViewModel result = map();

    QuestionRow row = result.getOtherRows().get(0);
    assertThat(row.getActiveRow().isTranslationComplete()).isTrue();
  }
}
