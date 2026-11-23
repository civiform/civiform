package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import models.ApplicationStep;
import models.DisplayMode;
import models.ProgramTab;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.admin.programs.ProgramIndexPageViewModel;
import views.admin.programs.ProgramIndexPageViewModel.Action;
import views.admin.programs.ProgramIndexPageViewModel.ModalModel;
import views.admin.programs.ProgramIndexPageViewModel.ProgramCard;
import views.admin.programs.ProgramIndexPageViewModel.ProgramRow;

public final class ProgramIndexPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";
  private static final Instant UPDATED_AT = Instant.parse("2024-01-01T17:00:00Z");
  private static final Instant UPDATED_EARLIER = Instant.parse("2023-06-15T17:00:00Z");

  private ProgramIndexPageMapper mapper;
  private DateConverter dateConverter;
  private ActiveAndDraftPrograms programs;
  private ActiveAndDraftQuestions questions;
  private List<String> programNames;
  private List<ProgramDefinition> draftPrograms;

  @Before
  public void setup() {
    mapper = new ProgramIndexPageMapper();

    dateConverter = mock(DateConverter.class);
    when(dateConverter.renderDateTimeHumanReadable(UPDATED_AT))
        .thenReturn("2024/01/01 at 9:00 AM PST");
    when(dateConverter.renderDate(UPDATED_AT)).thenReturn("2024/01/01");

    programNames = new ArrayList<>();
    draftPrograms = new ArrayList<>();
    programs = mock(ActiveAndDraftPrograms.class);
    when(programs.getProgramNames()).thenAnswer(call -> ImmutableSet.copyOf(programNames));
    when(programs.getDraftPrograms()).thenAnswer(call -> ImmutableList.copyOf(draftPrograms));

    questions = mock(ActiveAndDraftQuestions.class);
    when(questions.getDraftQuestions()).thenReturn(ImmutableList.of());
  }

  private static ProgramDefinition.Builder programBuilder(long id, String adminName) {
    return ProgramDefinition.builder()
        .setId(id)
        .setAdminName(adminName)
        .setAdminDescription("")
        .setLocalizedName(LocalizedStrings.withDefaultValue(adminName + " name"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue(adminName + " short"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setLastModifiedTime(UPDATED_AT)
        .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
        .setBridgeDefinitions(ImmutableMap.of());
  }

  private static BlockDefinition blockWithQuestionIds(long... questionIds) {
    ImmutableList.Builder<ProgramQuestionDefinition> programQuestions = ImmutableList.builder();
    for (long questionId : questionIds) {
      programQuestions.add(
          ProgramQuestionDefinition.create(
              universalQuestion(questionId), /* programDefinitionId= */ Optional.empty()));
    }

    return BlockDefinition.builder()
        .setId(1L)
        .setName("Screen")
        .setDescription("Screen description")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Screen"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen description"))
        .setProgramQuestionDefinitions(programQuestions.build())
        .build();
  }

  private static QuestionDefinition universalQuestion(long id) {
    QuestionDefinition question = mock(QuestionDefinition.class);
    when(question.getId()).thenReturn(id);
    when(question.isUniversal()).thenReturn(true);
    return question;
  }

  private static QuestionDefinition draftQuestion(long id, String name) {
    QuestionDefinition question = mock(QuestionDefinition.class);
    when(question.getId()).thenReturn(id);
    when(question.getName()).thenReturn(name);
    when(question.getQuestionText()).thenReturn(LocalizedStrings.withDefaultValue(name + " text"));
    return question;
  }

  /** Registers a program name with its active and draft versions, either of which may be null. */
  private void givenProgram(ProgramDefinition active, ProgramDefinition draft) {
    String name = active != null ? active.adminName() : draft.adminName();
    programNames.add(name);
    when(programs.getActiveProgramDefinition(name)).thenReturn(Optional.ofNullable(active));
    when(programs.getDraftProgramDefinition(name)).thenReturn(Optional.ofNullable(draft));

    if (draft != null) {
      draftPrograms.add(draft);
      when(programs.anyDraft()).thenReturn(true);
    }
  }

  private ProgramIndexPageMapper.Params.ParamsBuilder params() {
    return ProgramIndexPageMapper.Params.builder()
        .programs(programs)
        .allDraftPrograms(() -> ImmutableList.copyOf(draftPrograms))
        .questions(questions)
        .upToDateQuestions(ImmutableList.of())
        .selectedTab(ProgramTab.IN_USE)
        .anyDisabledPrograms(false)
        .civicEntityShortName("Seattle")
        .baseUrl(BASE_URL)
        .hasTranslatableLocales(true)
        .canViewApplications(program -> true)
        .isTranslationComplete(program -> true)
        .publicImageUrl(fileKey -> "https://images.example.com/" + fileKey)
        .dateConverter(dateConverter)
        .successMessage(Optional.empty())
        .errorMessage(Optional.empty());
  }

  private ProgramCard onlyCard(ProgramIndexPageViewModel model) {
    assertThat(model.getProgramCards()).hasSize(1);
    return model.getProgramCards().get(0);
  }

  private static ImmutableList<String> labels(ImmutableList<Action> actions) {
    return actions.stream().map(Action::getLabel).collect(ImmutableList.toImmutableList());
  }

  private static Action action(ImmutableList<Action> actions, String label) {
    return actions.stream().filter(a -> a.getLabel().equals(label)).findFirst().orElseThrow();
  }

  private static ModalModel modal(ProgramIndexPageViewModel model, ModalModel.Type type) {
    return model.getModals().stream()
        .filter(modal -> modal.getType().equals(type))
        .findFirst()
        .orElseThrow();
  }

  @Test
  public void map_setsHeadingsAndPageUrls() {
    ProgramIndexPageViewModel result = mapper.map(params().build());

    assertThat(result.getTitle()).isEqualTo("Program dashboard");
    assertThat(result.getExplanation()).isEqualTo("Create, edit and publish programs in Seattle");
    assertThat(result.getNewProgramUrl()).isEqualTo("/admin/programs/new");
    assertThat(result.getImportProgramUrl()).isEqualTo("/admin/import");
  }

  @Test
  public void map_flashMessages_buildDismissibleSuccessAndUndismissibleErrorToasts() {
    ProgramIndexPageViewModel result =
        mapper.map(
            params().successMessage(Optional.of("Saved")).errorMessage(Optional.of("Bad")).build());

    assertThat(result.getToasts()).hasSize(2);
    assertThat(result.getToasts().get(0).getMessage()).isEqualTo("Saved");
    assertThat(result.getToasts().get(0).getType()).isEqualTo("SUCCESS");
    assertThat(result.getToasts().get(0).isCanDismiss()).isTrue();
    assertThat(result.getToasts().get(1).getMessage()).isEqualTo("Error: Bad");
    assertThat(result.getToasts().get(1).getType()).isEqualTo("ERROR");
    assertThat(result.getToasts().get(1).isCanDismiss()).isFalse();
  }

  @Test
  public void map_noFlashMessages_hasNoToasts() {
    assertThat(mapper.map(params().build()).getToasts()).isEmpty();
  }

  @Test
  public void map_noDisabledPrograms_hasNoTabs() {
    assertThat(mapper.map(params().anyDisabledPrograms(false).build()).getTabs()).isEmpty();
  }

  @Test
  public void map_inUseTabSelected_marksOnlyInUseSelected() {
    ProgramIndexPageViewModel result =
        mapper.map(params().anyDisabledPrograms(true).selectedTab(ProgramTab.IN_USE).build());

    assertThat(result.getTabs()).hasSize(2);
    assertThat(result.getTabs().get(0).getLabel()).isEqualTo("In use");
    assertThat(result.getTabs().get(0).getUrl()).isEqualTo("/admin/programs");
    assertThat(result.getTabs().get(0).isSelected()).isTrue();
    assertThat(result.getTabs().get(1).getLabel()).isEqualTo("Disabled");
    assertThat(result.getTabs().get(1).getUrl()).isEqualTo("/admin/programs/disabled");
    assertThat(result.getTabs().get(1).isSelected()).isFalse();
  }

  @Test
  public void map_disabledTabSelected_marksOnlyDisabledSelected() {
    ProgramIndexPageViewModel result =
        mapper.map(params().anyDisabledPrograms(true).selectedTab(ProgramTab.DISABLED).build());

    assertThat(result.getTabs().get(0).isSelected()).isFalse();
    assertThat(result.getTabs().get(1).isSelected()).isTrue();
  }

  @Test
  public void map_sortsPreScreenerFirstThenMostRecentThenCaseInsensitiveName() {
    givenProgram(programBuilder(1L, "b-program").build(), null);
    givenProgram(programBuilder(2L, "A-program").build(), null);
    givenProgram(
        programBuilder(3L, "older-program").setLastModifiedTime(UPDATED_EARLIER).build(), null);
    givenProgram(
        programBuilder(4L, "pre-screener")
            .setProgramType(ProgramType.PRE_SCREENER_FORM)
            .setLastModifiedTime(UPDATED_EARLIER)
            .build(),
        null);

    ProgramIndexPageViewModel result = mapper.map(params().build());

    assertThat(result.getProgramCards().stream().map(ProgramCard::getTitle))
        .containsExactly(
            "pre-screener name", "A-program name", "b-program name", "older-program name");
  }

  @Test
  public void map_draftAndActive_cardShowsDraftDetailsAndActiveRowIsSecond() {
    ProgramDefinition active = programBuilder(1L, "program").build();
    ProgramDefinition draft =
        programBuilder(2L, "program")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Draft name"))
            .setAdminDescription("Draft note")
            .build();
    givenProgram(active, draft);

    ProgramCard card = onlyCard(mapper.map(params().build()));

    assertThat(card.getTitle()).isEqualTo("Draft name");
    assertThat(card.getAdminNote()).isEqualTo("Draft note");
    assertThat(card.getDraftRow().getId()).isEqualTo(2L);
    assertThat(card.getDraftRow().isActive()).isFalse();
    assertThat(card.getDraftRow().isSecondRow()).isFalse();
    assertThat(card.getActiveRow().getId()).isEqualTo(1L);
    assertThat(card.getActiveRow().isActive()).isTrue();
    assertThat(card.getActiveRow().isSecondRow()).isTrue();
  }

  @Test
  public void map_activeOnly_hasNoDraftRowAndActiveRowIsNotSecond() {
    givenProgram(programBuilder(1L, "program").build(), null);

    ProgramCard card = onlyCard(mapper.map(params().build()));

    assertThat(card.getDraftRow()).isNull();
    assertThat(card.getActiveRow().isSecondRow()).isFalse();
  }

  @Test
  public void map_draftOnly_hasNoActiveRow() {
    givenProgram(null, programBuilder(1L, "program").build());

    assertThat(onlyCard(mapper.map(params().build())).getActiveRow()).isNull();
  }

  @Test
  public void map_blankAdminNote_isNull() {
    givenProgram(programBuilder(1L, "program").setAdminDescription("  ").build(), null);

    assertThat(onlyCard(mapper.map(params().build())).getAdminNote()).isNull();
  }

  @Test
  public void map_noCategories_showsNone() {
    givenProgram(programBuilder(1L, "program").build(), null);

    assertThat(onlyCard(mapper.map(params().build())).getCategoriesText()).isEqualTo("None");
  }

  @Test
  public void map_rowSetsFormattedDatesAndCounts() {
    givenProgram(
        programBuilder(1L, "program")
            .setBlockDefinitions(ImmutableList.of(blockWithQuestionIds(10L, 11L)))
            .build(),
        null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(row.getUpdatedDateTime()).isEqualTo("2024/01/01 at 9:00 AM PST");
    assertThat(row.getUpdatedDate()).isEqualTo("2024/01/01");
    assertThat(row.getBlockCount()).isEqualTo(1);
    assertThat(row.getQuestionCount()).isEqualTo(2);
  }

  @Test
  public void map_noLastModifiedTime_rendersUnknownDates() {
    ProgramDefinition program = programBuilder(1L, "program").setLastModifiedTime(null).build();
    givenProgram(program, null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(row.getUpdatedDateTime()).isEqualTo("unknown");
    assertThat(row.getUpdatedDate()).isEqualTo("unknown");
  }

  @Test
  public void map_translationIncomplete_marksRowIncomplete() {
    givenProgram(programBuilder(1L, "program").build(), null);

    ProgramRow row =
        onlyCard(mapper.map(params().isTranslationComplete(program -> false).build()))
            .getActiveRow();

    assertThat(row.isTranslationComplete()).isFalse();
  }

  @Test
  public void map_validImageKey_setsImageUrlAndNameAsAlt() {
    givenProgram(
        programBuilder(1L, "program")
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-1/a.png"))
            .build(),
        null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(row.getImageUrl())
        .isEqualTo("https://images.example.com/program-summary-image/program-1/a.png");
    assertThat(row.getImageAlt()).isEqualTo("program name");
  }

  @Test
  public void map_imageKeyWithWrongPrefix_hasNoImage() {
    givenProgram(
        programBuilder(1L, "program")
            .setSummaryImageFileKey(Optional.of("question-image/question-1/a.png"))
            .build(),
        null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(row.getImageUrl()).isNull();
    assertThat(row.getImageAlt()).isNull();
  }

  @Test
  public void map_noImageKey_hasNoImage() {
    givenProgram(programBuilder(1L, "program").build(), null);

    assertThat(onlyCard(mapper.map(params().build())).getActiveRow().getImageUrl()).isNull();
  }

  @Test
  public void map_draftRow_hasPublishAndEditThenExtraActionsInLegacyOrder() {
    givenProgram(null, programBuilder(7L, "my-program").build());

    ProgramRow row = onlyCard(mapper.map(params().build())).getDraftRow();

    assertThat(labels(row.getRowActions())).containsExactly("Publish", "Edit");
    assertThat(row.getRowActions().get(0).getId()).isEqualTo("publish-modal-my-program-button");
    assertThat(row.getRowActions().get(1).getId()).isEqualTo("program-edit-link-7");
    assertThat(row.getRowActions().get(1).getRedirectUrl()).isEqualTo("/admin/programs/7/blocks");
    assertThat(labels(row.getExtraActions()))
        .containsExactly(
            "Manage program admins",
            "Manage translations",
            "Manage application statuses",
            "Export program");
    assertThat(row.getHasExtraActions()).isTrue();
  }

  @Test
  public void map_activeRowWithoutDraft_hasViewShareAndNewVersionEditPost() {
    givenProgram(programBuilder(7L, "my-program").build(), null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(labels(row.getRowActions())).containsExactly("View", "Share link");
    assertThat(action(row.getRowActions(), "Share link").getCopyableLink())
        .isEqualTo(BASE_URL + "/programs/my-program");
    assertThat(labels(row.getExtraActions()))
        .containsExactly(
            "Applications",
            "Edit",
            "Manage translations",
            "Manage program admins",
            "Export program");

    Action edit = action(row.getExtraActions(), "Edit");
    assertThat(edit.getId()).isEqualTo("program-new-version-link-7");
    assertThat(edit.getFormId()).isEqualTo("program-new-version-form-7");
    assertThat(edit.getPostUrl()).isEqualTo("/admin/programs/7/newVersion");
    assertThat(edit.getRedirectUrl()).isNull();
  }

  @Test
  public void map_activeRowWithDraft_omitsEditAndManageTranslations() {
    givenProgram(programBuilder(1L, "program").build(), programBuilder(2L, "program").build());

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(labels(row.getExtraActions()))
        .containsExactly("Applications", "Manage program admins", "Export program");
  }

  @Test
  public void map_noTranslatableLocales_omitsManageTranslations() {
    givenProgram(null, programBuilder(1L, "program").build());

    ProgramRow row =
        onlyCard(mapper.map(params().hasTranslatableLocales(false).build())).getDraftRow();

    assertThat(labels(row.getExtraActions())).doesNotContain("Manage translations");
  }

  @Test
  public void map_cannotViewApplications_omitsApplicationsAction() {
    givenProgram(programBuilder(1L, "program").build(), null);

    ProgramRow row =
        onlyCard(mapper.map(params().canViewApplications(program -> false).build())).getActiveRow();

    assertThat(labels(row.getExtraActions())).doesNotContain("Applications");
  }

  @Test
  public void map_preScreenerForm_labelsApplicationsActionForms() {
    givenProgram(
        programBuilder(1L, "program").setProgramType(ProgramType.PRE_SCREENER_FORM).build(), null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(labels(row.getExtraActions())).contains("Forms").doesNotContain("Applications");
  }

  @Test
  public void map_externalDraftRow_onlyOffersManageTranslations() {
    givenProgram(null, programBuilder(1L, "program").setProgramType(ProgramType.EXTERNAL).build());

    ProgramRow row = onlyCard(mapper.map(params().build())).getDraftRow();

    assertThat(labels(row.getExtraActions())).containsExactly("Manage translations");
  }

  @Test
  public void map_externalActiveRow_hasNoShareLinkOrApplicationsAdminsAndExportActions() {
    givenProgram(programBuilder(1L, "program").setProgramType(ProgramType.EXTERNAL).build(), null);

    ProgramRow row = onlyCard(mapper.map(params().build())).getActiveRow();

    assertThat(labels(row.getRowActions())).containsExactly("View");
    assertThat(labels(row.getExtraActions())).containsExactly("Edit", "Manage translations");
  }

  @Test
  public void map_externalDraftWithoutTranslatableLocales_hasNoExtraActions() {
    givenProgram(null, programBuilder(1L, "program").setProgramType(ProgramType.EXTERNAL).build());

    ProgramRow row =
        onlyCard(mapper.map(params().hasTranslatableLocales(false).build())).getDraftRow();

    assertThat(row.getHasExtraActions()).isFalse();
  }

  @Test
  public void map_noUniversalQuestions_hasNoUniversalText() {
    givenProgram(programBuilder(1L, "program").build(), null);

    assertThat(onlyCard(mapper.map(params().build())).getActiveRow().getUniversalQuestionsText())
        .isNull();
  }

  @Test
  public void map_programUsesAllUniversalQuestions_saysAll() {
    givenProgram(
        programBuilder(1L, "program")
            .setBlockDefinitions(ImmutableList.of(blockWithQuestionIds(10L, 11L)))
            .build(),
        null);

    ProgramRow row =
        onlyCard(
                mapper.map(
                    params()
                        .upToDateQuestions(
                            ImmutableList.of(universalQuestion(10L), universalQuestion(11L)))
                        .build()))
            .getActiveRow();

    assertThat(row.getUniversalQuestionsText()).isEqualTo("Contains all universal questions ");
  }

  @Test
  public void map_programUsesSomeUniversalQuestions_saysCountOfTotal() {
    givenProgram(
        programBuilder(1L, "program")
            .setBlockDefinitions(ImmutableList.of(blockWithQuestionIds(10L)))
            .build(),
        null);

    ProgramRow row =
        onlyCard(
                mapper.map(
                    params()
                        .upToDateQuestions(
                            ImmutableList.of(universalQuestion(10L), universalQuestion(11L)))
                        .build()))
            .getActiveRow();

    assertThat(row.getUniversalQuestionsText()).isEqualTo("Contains 1 of 2 universal questions ");
  }

  @Test
  public void map_externalProgram_hasNoUniversalTextEvenWithUniversalQuestions() {
    givenProgram(programBuilder(1L, "program").setProgramType(ProgramType.EXTERNAL).build(), null);

    ProgramRow row =
        onlyCard(
                mapper.map(
                    params().upToDateQuestions(ImmutableList.of(universalQuestion(10L))).build()))
            .getActiveRow();

    assertThat(row.getUniversalQuestionsText()).isNull();
  }

  @Test
  public void map_alwaysIncludesDemographicsModalFirst() {
    ProgramIndexPageViewModel result = mapper.map(params().build());

    assertThat(result.getModals()).hasSize(1);
    ModalModel demographics = result.getModals().get(0);
    assertThat(demographics.getType()).isEqualTo(ModalModel.Type.DEMOGRAPHICS_CSV);
    assertThat(demographics.getId()).isEqualTo("download-demographics-csv-modal");
    assertThat(demographics.getWidthClass()).isEqualTo("lg:w-auto");
    assertThat(demographics.getActionUrl()).startsWith("/admin/demographics");
  }

  @Test
  public void map_defaultDraftProgram_publishModalMentionsDraftQuestions() {
    givenProgram(null, programBuilder(5L, "my-program").build());

    ModalModel publish = modal(mapper.map(params().build()), ModalModel.Type.PUBLISH_PROGRAM);

    assertThat(publish.getId()).isEqualTo("publish-modal-my-program");
    assertThat(publish.getWidthClass()).isEqualTo("lg:w-1/3");
    assertThat(publish.getActionUrl()).isEqualTo("/admin/programs/5/publish");
    assertThat(publish.getTitle())
        .isEqualTo(
            "Are you sure you want to publish my-program name and all of its draft questions?");
  }

  @Test
  public void map_externalDraftProgram_publishModalOmitsDraftQuestions() {
    givenProgram(
        null, programBuilder(5L, "my-program").setProgramType(ProgramType.EXTERNAL).build());

    ModalModel publish = modal(mapper.map(params().build()), ModalModel.Type.PUBLISH_PROGRAM);

    assertThat(publish.getTitle()).isEqualTo("Are you sure you want to publish my-program name?");
  }

  @Test
  public void map_draftMissingUniversalQuestion_publishModalWarns() {
    givenProgram(null, programBuilder(5L, "my-program").build());

    ModalModel publish =
        modal(
            mapper.map(
                params().upToDateQuestions(ImmutableList.of(universalQuestion(10L))).build()),
            ModalModel.Type.PUBLISH_PROGRAM);

    assertThat(publish.isMissingUniversalQuestions()).isTrue();
  }

  @Test
  public void map_draftWithAllUniversalQuestions_publishModalDoesNotWarn() {
    givenProgram(
        null,
        programBuilder(5L, "my-program")
            .setBlockDefinitions(ImmutableList.of(blockWithQuestionIds(10L)))
            .build());

    ModalModel publish =
        modal(
            mapper.map(
                params().upToDateQuestions(ImmutableList.of(universalQuestion(10L))).build()),
            ModalModel.Type.PUBLISH_PROGRAM);

    assertThat(publish.isMissingUniversalQuestions()).isFalse();
  }

  @Test
  public void map_externalDraft_publishModalDoesNotWarnAboutUniversalQuestions() {
    givenProgram(
        null, programBuilder(5L, "my-program").setProgramType(ProgramType.EXTERNAL).build());

    ModalModel publish =
        modal(
            mapper.map(
                params().upToDateQuestions(ImmutableList.of(universalQuestion(10L))).build()),
            ModalModel.Type.PUBLISH_PROGRAM);

    assertThat(publish.isMissingUniversalQuestions()).isFalse();
  }

  @Test
  public void map_noDraftsOrQuestionEdits_hasNoPublishAllAndSkipsLoadingAllDrafts() {
    AtomicBoolean loadedAllDrafts = new AtomicBoolean(false);

    ProgramIndexPageViewModel result =
        mapper.map(
            params()
                .allDraftPrograms(
                    () -> {
                      loadedAllDrafts.set(true);
                      return ImmutableList.of();
                    })
                .build());

    assertThat(result.getPublishAllTriggerId()).isNull();
    assertThat(result.getModals().stream().map(ModalModel::getType))
        .doesNotContain(ModalModel.Type.PUBLISH_ALL);
    assertThat(loadedAllDrafts).isFalse();
  }

  @Test
  public void map_onlyDraftQuestionEdits_showsPublishAllWithNoPrograms() {
    when(questions.draftVersionHasAnyEdits()).thenReturn(true);
    ImmutableList<QuestionDefinition> draftQuestions = ImmutableList.of(draftQuestion(3L, "q"));
    when(questions.getDraftQuestions()).thenReturn(draftQuestions);

    ProgramIndexPageViewModel result = mapper.map(params().build());

    assertThat(result.getPublishAllTriggerId()).isEqualTo("publish-all-programs-modal-button");
    ModalModel publishAll = modal(result, ModalModel.Type.PUBLISH_ALL);
    assertThat(publishAll.getProgramsHeading()).isEqualTo("0 draft programs will be published:");
    assertThat(publishAll.getDraftPrograms()).isEmpty();
    assertThat(publishAll.getQuestionsHeading()).isEqualTo("1 draft question will be published:");
  }

  @Test
  public void map_publishAll_listsAllDraftsSortedIncludingOnesNotOnTheTab() {
    givenProgram(null, programBuilder(1L, "b-program").build());
    ProgramDefinition disabledDraft =
        programBuilder(2L, "a-disabled").setDisplayMode(DisplayMode.DISABLED).build();

    ModalModel publishAll =
        modal(
            mapper.map(
                params()
                    .allDraftPrograms(
                        () ->
                            ImmutableList.<ProgramDefinition>builder()
                                .addAll(draftPrograms)
                                .add(disabledDraft)
                                .build())
                    .build()),
            ModalModel.Type.PUBLISH_ALL);

    assertThat(publishAll.getProgramsHeading()).isEqualTo("2 draft programs will be published:");
    assertThat(publishAll.getDraftPrograms().stream().map(p -> p.getName()))
        .containsExactly("a-disabled name", "b-program name");
    assertThat(publishAll.getDraftPrograms().get(0).getEditUrl())
        .isEqualTo("/admin/programs/2/edit/EDIT");
  }

  @Test
  public void map_publishAll_setsVisibilityTextForEachDisplayMode() {
    givenProgram(null, programBuilder(1L, "a").setDisplayMode(DisplayMode.DISABLED).build());
    givenProgram(null, programBuilder(2L, "b").setDisplayMode(DisplayMode.HIDDEN_IN_INDEX).build());
    givenProgram(null, programBuilder(3L, "c").setDisplayMode(DisplayMode.PUBLIC).build());
    givenProgram(null, programBuilder(4L, "d").setDisplayMode(DisplayMode.TI_ONLY).build());
    givenProgram(null, programBuilder(5L, "e").setDisplayMode(DisplayMode.SELECT_TI).build());

    ModalModel publishAll = modal(mapper.map(params().build()), ModalModel.Type.PUBLISH_ALL);

    assertThat(publishAll.getDraftPrograms().stream().map(p -> p.getVisibilityText()))
        .containsExactly(
            " (Hidden from applicants and Trusted Intermediaries) ",
            " (Hidden from applicants) ",
            " (Publicly visible) ",
            " ",
            " ");
  }

  @Test
  public void map_publishAll_sortsQuestionsByNameAndKeepsNameForTestId() {
    ImmutableList<QuestionDefinition> draftQuestions =
        ImmutableList.of(draftQuestion(2L, "z-question"), draftQuestion(1L, "a-question"));
    when(questions.getDraftQuestions()).thenReturn(draftQuestions);
    when(questions.draftVersionHasAnyEdits()).thenReturn(true);

    ModalModel publishAll = modal(mapper.map(params().build()), ModalModel.Type.PUBLISH_ALL);

    assertThat(publishAll.getQuestionsHeading()).isEqualTo("2 draft questions will be published:");
    assertThat(publishAll.getDraftQuestions().get(0).getAdminName()).isEqualTo("a-question");
    assertThat(publishAll.getDraftQuestions().get(0).getText()).isEqualTo("a-question text");
    assertThat(publishAll.getDraftQuestions().get(0).getEditUrl())
        .startsWith("/admin/questions/1/edit");
  }
}
