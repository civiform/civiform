package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.CreateQuestionButton;
import views.admin.questions.NotifySharedQuestionView;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for viewing all active questions and draft questions. */
public final class QuestionsListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final TranslationLocales translationLocales;
  private final ViewUtils viewUtils;
  private final Logger logger = LoggerFactory.getLogger(QuestionsListView.class);

  @Inject
  public QuestionsListView(
      AdminLayoutFactory layoutFactory,
      TranslationLocales translationLocales,
      ViewUtils viewUtils) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    this.translationLocales = checkNotNull(translationLocales);
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Renders a page with a list view of all questions. */
  public Content render(ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    String title = "All Questions";

    Pair<DivTag, ImmutableList<Modal>> questionRowsAndModals =
        renderAllQuestionRows(activeAndDraftQuestions, request);

    DivTag contentDiv =
        div()
            .withClasses(Styles.PX_4)
            .with(
                div()
                    .withClasses(Styles.FLEX, Styles.ITEMS_CENTER, Styles.SPACE_X_4, Styles.MT_12)
                    .with(
                        h1(title),
                        div().withClass(Styles.FLEX_GROW),
                        CreateQuestionButton.renderCreateQuestionButton(
                            controllers.admin.routes.AdminQuestionController.index().url())),
                div()
                    .withClasses(Styles.MT_10, Styles.FLEX)
                    .with(
                        div().withClass(Styles.FLEX_GROW),
                        p("Sorting by most recently updated").withClass(Styles.TEXT_SM)))
            .with(div().withClass(Styles.MT_6).with(questionRowsAndModals.getLeft()))
            .with(renderSummary(activeAndDraftQuestions));
    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addModals(questionRowsAndModals.getRight())
            .addMainContent(contentDiv);

    Http.Flash flash = request.flash();
    if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(
          ToastMessage.success(flash.get("success").get()).setDismissible(false));
    } else if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(
          ToastMessage.error(flash.get("error").get()).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderSummary(ActiveAndDraftQuestions activeAndDraftQuestions) {
    // The total question count should be equivalent to the number of rows in the displayed table,
    // where we have a single entry for a question that is active and has a draft.
    return div(String.format(
            "Total Questions: %d", activeAndDraftQuestions.getQuestionNames().size()))
        .withClasses(Styles.FLOAT_RIGHT, Styles.TEXT_BASE, Styles.PX_4, Styles.MY_2);
  }

  private static QuestionDefinition getDisplayQuestion(QuestionCardData cardData) {
    return cardData.draftQuestion().orElseGet(cardData.activeQuestion()::get);
  }

  private Pair<DivTag, ImmutableList<Modal>> renderAllQuestionRows(
      ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    ImmutableList.Builder<DomContent> rows = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    ImmutableList<QuestionCardData> cards =
        activeAndDraftQuestions.getQuestionNames().stream()
            .map(
                name ->
                    QuestionCardData.builder()
                        .setActiveQuestion(
                            activeAndDraftQuestions.getActiveQuestionDefinition(name))
                        .setDraftQuestion(activeAndDraftQuestions.getDraftQuestionDefinition(name))
                        .build())
            .sorted(
                Comparator.<QuestionCardData, Instant>comparing(
                        card ->
                            getDisplayQuestion(card).getLastModifiedTime().orElse(Instant.EPOCH))
                    .reversed()
                    .thenComparing(
                        card ->
                            getDisplayQuestion(card).getQuestionText().getDefault().toLowerCase()))
            .collect(ImmutableList.toImmutableList());

    for (QuestionCardData card : cards) {
      Pair<DivTag, ImmutableList<Modal>> rowAndModals =
          renderQuestionCard(card, activeAndDraftQuestions, request);
      rows.add(rowAndModals.getLeft());
      modals.addAll(rowAndModals.getRight());
    }
    return Pair.of(div().with(rows.build()), modals.build());
  }

  @AutoValue
  abstract static class QuestionCardData {
    abstract Optional<QuestionDefinition> activeQuestion();

    abstract Optional<QuestionDefinition> draftQuestion();

    static Builder builder() {
      return new AutoValue_QuestionsListView_QuestionCardData.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setActiveQuestion(Optional<QuestionDefinition> v);

      abstract Builder setDraftQuestion(Optional<QuestionDefinition> v);

      abstract QuestionCardData build();
    }
  }

  /**
   * Renders a card in the question list. The card contains question text, help text, active and
   * draft versions, list of programs questions is used in and buttons to edit the question.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderQuestionCard(
      QuestionCardData cardData,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    if (cardData.draftQuestion().isEmpty() && cardData.activeQuestion().isEmpty()) {
      throw new IllegalArgumentException("Did not receive a valid question.");
    }
    QuestionDefinition latestDefinition =
        cardData.draftQuestion().orElseGet(cardData.activeQuestion()::get);

    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    Pair<DivTag, ImmutableList<Modal>> referencingProgramAndModal =
        renderReferencingPrograms(latestDefinition.getName(), activeAndDraftQuestions);
    modals.addAll(referencingProgramAndModal.getRight());

    DivTag row =
        div()
            .withClasses(Styles.FLEX)
            .with(renderInfoCell(latestDefinition))
            .with(referencingProgramAndModal.getLeft());

    DivTag draftAndActiveRows = div().withClasses(Styles.FLEX_GROW);
    if (cardData.draftQuestion().isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> draftRow =
          renderActiveOrDraftRow(
              /* isActive= */ false,
              cardData.draftQuestion().get(),
              activeAndDraftQuestions,
              request);
      modals.addAll(draftRow.getRight());
      draftAndActiveRows.with(draftRow.getLeft());
    }
    if (cardData.activeQuestion().isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> activeRow =
          renderActiveOrDraftRow(
              /* isActive= */ true,
              cardData.activeQuestion().get(),
              activeAndDraftQuestions,
              request);
      modals.addAll(activeRow.getRight());
      draftAndActiveRows.with(activeRow.getLeft());
    }
    row.with(draftAndActiveRows);

    DivTag adminNote =
        div()
            .withClasses(Styles.PY_7)
            .with(
                span("Admin ID: ").withClasses(Styles.FONT_BOLD),
                span(latestDefinition.getName()),
                br(),
                span("Admin note: ").withClasses(Styles.FONT_BOLD),
                span(latestDefinition.getDescription()));

    DivTag rowWithAdminNote =
        div()
            .withClasses(
                Styles.W_FULL,
                Styles.MY_4,
                Styles.PL_6,
                Styles.BORDER_GRAY_300,
                Styles.ROUNDED_LG,
                Styles.BORDER,
                ReferenceClasses.ADMIN_QUESTION_TABLE_ROW)
            .with(row)
            .with(adminNote);
    return Pair.of(rowWithAdminNote, modals.build());
  }

  /**
   * Renders single "draft" or "active" row within a question row. Question can have up to two such
   * rows, one "draft" and one "active".
   */
  private Pair<DivTag, ImmutableList<Modal>> renderActiveOrDraftRow(
      boolean isActive,
      QuestionDefinition question,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    boolean isSecondRow =
        isActive
            && activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isPresent();

    Pair<DivTag, ImmutableList<Modal>> actionsCellAndModal =
        renderActionsCell(isActive, question, activeAndDraftQuestions, request);

    PTag badge =
        ViewUtils.makeBadge(
            isActive ? BadgeStatus.ACTIVE : BadgeStatus.DRAFT,
            Styles.ML_2,
            StyleUtils.responsiveXLarge(Styles.ML_8));

    DivTag row =
        div()
            .withClasses(
                Styles.PY_7,
                Styles.FLEX,
                Styles.FLEX_ROW,
                Styles.ITEMS_CENTER,
                StyleUtils.hover(Styles.BG_GRAY_100),
                Styles.CURSOR_POINTER,
                isSecondRow ? Styles.BORDER_T : "")
            .with(badge)
            .with(div().withClasses(Styles.FLEX_GROW))
            .with(
                div()
                    .withClasses(Styles.ML_4, StyleUtils.responsiveXLarge(Styles.ML_10))
                    .with(viewUtils.renderEditOnText("Edited on ", question.getLastModifiedTime())))
            .with(actionsCellAndModal.getLeft());

    asRedirectElement(
        row, controllers.admin.routes.AdminQuestionController.show(question.getId()).url());

    return Pair.of(row, actionsCellAndModal.getRight());
  }

  private DivTag renderInfoCell(QuestionDefinition definition) {
    DivTag questionText =
        div()
            .withClasses(
                Styles.FONT_BOLD,
                Styles.TEXT_BLACK,
                Styles.FLEX,
                Styles.FLEX_ROW,
                Styles.ITEMS_CENTER)
            .with(
                Icons.questionTypeSvg(definition.getQuestionType())
                    .withClasses(Styles.W_6, Styles.H_6, Styles.FLEX_SHRINK_0))
            .with(
                div(definition.getQuestionText().getDefault())
                    .withClasses(
                        ReferenceClasses.ADMIN_QUESTION_TITLE, Styles.PL_4, Styles.TEXT_XL));
    DivTag questionDescription =
        div(
            div(definition.getQuestionHelpText().isEmpty()
                    ? ""
                    : definition.getQuestionHelpText().getDefault())
                .withClasses(Styles.PL_10));
    return div()
        .withClasses(
            Styles.PY_7, Styles.W_1_4, Styles.FLEX, Styles.FLEX_COL, Styles.JUSTIFY_BETWEEN)
        .with(div().with(questionText).with(questionDescription));
  }

  private Optional<Modal> maybeRenderEditModal(
      QuestionDefinition question,
      ActiveAndDraftQuestions activeAndDraftQuestions) {

    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        activeAndDraftQuestions.getReferencingPrograms(question.getName());
    Collection<ProgramDefinition> activePrograms = referencingPrograms.activeReferences();

    if (activePrograms.size() == 0) {
      return Optional.empty();
    }

    //String link = controllers.admin.routes.AdminQuestionController.edit(question.getId()).url();
    //ButtonTag editButton =
    //  renderQuestionEditLink(question, activeAndDraftQuestions, request, link);

    //String title = "This question is shared by 2 programs";
    DivTag notifyContent =
        div()
            .with(
                h1(
                    "This question is shared by 2 programs. If you edit it, it will be updated for"
                        + " both programs."),
                p(
                    "Please be aware that this will effect the following programs by either"
                        + " creating a new draft with this change or updating an existing draft:"),
                div().with(span("Program name 1"), span("Program name 2")));

    Modal editQuestionModal = Modal.builder("edit-question", notifyContent)
      .setModalTitle("Editing a shared question")
      .build();

    return Optional.of(editQuestionModal);
  }

  private Pair<ButtonTag, Optional<Modal>> getEditButtonAndModal(
      ActiveAndDraftQuestions activeAndDraftQuestions,
      QuestionDefinition definition,
      boolean isVisible) {

    Optional<Modal> maybeEditModal =
        maybeRenderEditModal(definition, activeAndDraftQuestions);

    return renderQuestionEditLink(definition, maybeEditModal, isVisible);
  }

  /**
   * Renders text describing how programs use specified question and provides a link to show dialog
   * listing all such programs.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderReferencingPrograms(
      String questionName, ActiveAndDraftQuestions activeAndDraftQuestions) {
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        activeAndDraftQuestions.getReferencingPrograms(questionName);
    Collection<ProgramDefinition> activePrograms = referencingPrograms.activeReferences();
    Collection<ProgramDefinition> draftPrograms = referencingPrograms.draftReferences();
    GroupedReferencingPrograms groupedReferencingPrograms =
        createReferencingPrograms(activePrograms, draftPrograms);

    Optional<Modal> maybeReferencingProgramsModal =
        makeReferencingProgramsModal(
            questionName, groupedReferencingPrograms, /* modalHeader= */ Optional.empty());

    DivTag tag =
        div()
            .withClasses(
                ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS,
                Styles.ML_4,
                StyleUtils.responsiveXLarge(Styles.ML_10),
                Styles.PY_7,
                Styles.W_1_4);
    if (groupedReferencingPrograms.isEmpty()) {
      tag.with(p("Used in 0 programs."));
    } else {
      if (!groupedReferencingPrograms.usedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.usedPrograms().size();
        tag.with(p("Used in " + numPrograms + " program" + (numPrograms > 1 ? "s." : ".")));
      }
      if (!groupedReferencingPrograms.addedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.addedPrograms().size();
        tag.with(p("Added to " + numPrograms + " program" + (numPrograms > 1 ? "s." : ".")));
      }
      if (!groupedReferencingPrograms.removedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.removedPrograms().size();
        tag.with(p("Removed from " + numPrograms + " program" + (numPrograms > 1 ? "s." : ".")));
      }
    }
    if (maybeReferencingProgramsModal.isPresent()) {
      tag.with(
          a().withId(maybeReferencingProgramsModal.get().getTriggerButtonId())
              .withClasses(
                  Styles.CURSOR_POINTER,
                  Styles.FONT_MEDIUM,
                  Styles.UNDERLINE,
                  BaseStyles.TEXT_SEATTLE_BLUE,
                  StyleUtils.hover(Styles.TEXT_BLACK))
              .withText("See list"));
    }
    return Pair.of(
        tag, maybeReferencingProgramsModal.map(ImmutableList::of).orElse(ImmutableList.of()));
  }

  @AutoValue
  abstract static class GroupedReferencingPrograms {
    abstract ImmutableList<ProgramDefinition> usedPrograms();

    abstract ImmutableList<ProgramDefinition> addedPrograms();

    abstract ImmutableList<ProgramDefinition> removedPrograms();

    static Builder builder() {
      return new AutoValue_QuestionsListView_GroupedReferencingPrograms.Builder();
    }

    boolean isEmpty() {
      return usedPrograms().isEmpty() && addedPrograms().isEmpty() && removedPrograms().isEmpty();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setUsedPrograms(ImmutableList<ProgramDefinition> usedPrograms);

      abstract Builder setAddedPrograms(ImmutableList<ProgramDefinition> addedPrograms);

      abstract Builder setRemovedPrograms(ImmutableList<ProgramDefinition> removedPrograms);

      abstract GroupedReferencingPrograms build();
    }
  }

  private GroupedReferencingPrograms createReferencingPrograms(
      Collection<ProgramDefinition> activePrograms, Collection<ProgramDefinition> draftPrograms) {
    ImmutableMap<String, ProgramDefinition> activeProgramsMap =
        activePrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));
    ImmutableMap<String, ProgramDefinition> draftProgramsMap =
        draftPrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    // Use set operations to collect programs into 3 sets.
    Set<String> usedSet = Sets.intersection(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> addedSet = Sets.difference(draftProgramsMap.keySet(), activeProgramsMap.keySet());
    Set<String> removedSet = Sets.difference(activeProgramsMap.keySet(), draftProgramsMap.keySet());

    ImmutableList<ProgramDefinition> usedPrograms =
        usedSet.stream()
            .map((adminName) -> draftProgramsMap.get(adminName))
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> addedPrograms =
        addedSet.stream()
            .map((adminName) -> draftProgramsMap.get(adminName))
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> removedPrograms =
        removedSet.stream()
            .map((adminName) -> activeProgramsMap.get(adminName))
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    return GroupedReferencingPrograms.builder()
        .setUsedPrograms(usedPrograms)
        .setAddedPrograms(addedPrograms)
        .setRemovedPrograms(removedPrograms)
        .build();
  }

  private Optional<Modal> makeReferencingProgramsModal(
      String questionName,
      GroupedReferencingPrograms referencingPrograms,
      Optional<DomContent> modalHeader) {
    if (referencingPrograms.isEmpty()) {
      return Optional.empty();
    }

    DivTag referencingProgramModalContent =
        div().withClasses(Styles.P_6, Styles.FLEX_ROW, Styles.SPACE_Y_6);
    if (modalHeader.isPresent()) {
      referencingProgramModalContent.with(modalHeader.get());
    }
    referencingProgramModalContent
        .condWith(
            !referencingPrograms.usedPrograms().isEmpty(),
            div()
                .with(
                    referencingProgramList(
                        "This question is used in:", referencingPrograms.usedPrograms()))
                .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_USED))
        .condWith(
            !referencingPrograms.addedPrograms().isEmpty(),
            div()
                .with(
                    referencingProgramList(
                        "This question is added to:", referencingPrograms.addedPrograms()))
                .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_ADDED))
        .condWith(
            !referencingPrograms.removedPrograms().isEmpty(),
            div()
                .with(
                    referencingProgramList(
                        "This question is removed from:", referencingPrograms.removedPrograms()))
                .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_REMOVED))
        .with(
            p("Note: This list does not automatically refresh. If edits are made to a program"
                    + " in a separate tab, they won't be reflected until the page has been"
                    + " refreshed.")
                .withClass(Styles.TEXT_SM));

    return Optional.of(
        Modal.builder(Modal.randomModalId(), referencingProgramModalContent)
            .setModalTitle(String.format("Programs referencing %s", questionName))
            .setWidth(Width.HALF)
            .build());
  }

  private DivTag referencingProgramList(
      String title, ImmutableList<ProgramDefinition> referencingPrograms) {
    // TODO(#3162): Add ability to view a published program. Then add
    // links to the specific block that references the question.
    return div()
        .with(p(title).withClass(Styles.FONT_SEMIBOLD))
        .with(
            div()
                .with(
                    ul().withClasses(Styles.LIST_DISC, Styles.LIST_INSIDE)
                        .with(
                            each(
                                referencingPrograms,
                                programReference -> {
                                  return li(programReference.adminName());
                                }))));
  }

  //private ButtonTag renderNotifySharedQuestionLink(
  //    QuestionDefinition definition, boolean isVisible) {
  //  String link =
  //      controllers.admin.routes.AdminProgramController.notifyQuestionShared(definition.getId())
  //          .url();
  //  return renderQuestionEditLink(link, isVisible);
  //}

  private Pair<ButtonTag, Optional<Modal>> renderQuestionEditLink(
      QuestionDefinition definition, 
      Optional<Modal> maybeEditModal, 
      boolean isVisible) {
    String link = controllers.admin.routes.AdminQuestionController.edit(definition.getId()).url();
    ButtonTag editButton = asRedirectElement(
        makeSvgTextButton("Edit", Icons.EDIT)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES, isVisible ? "" : Styles.INVISIBLE),
            //.withCondId(maybeEditModal.isPresent(), maybeEditModal.get().getTriggerButtonId()),
        link);

    return Pair.of(editButton, maybeEditModal);
  }

  private Optional<ButtonTag> renderQuestionTranslationLink(QuestionDefinition definition) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String link =
        controllers.admin.routes.AdminQuestionTranslationsController.redirectToFirstLocale(
                definition.getId())
            .url();

    ButtonTag button =
        asRedirectElement(
            makeSvgTextButton("Manage Translations", Icons.TRANSLATE)
                .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
            link);
    return Optional.of(button);
  }

  private Pair<DivTag, ImmutableList<Modal>> renderActionsCell(
      boolean isActive,
      QuestionDefinition question,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    ImmutableList.Builder<DomContent> extraActions = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    // some actions such as "edit" or "archive" need to be rendered only on one of two rows.
    // If there is only "draft" or only "active" rows - render these actions.
    // If there are both "draft" and "active" versions - render edit actions only on "draft".
    boolean isEditable =
        !isActive
            || activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isEmpty();

    if (!isActive) {
      Optional<ButtonTag> maybeTranslationLink = renderQuestionTranslationLink(question);
      maybeTranslationLink.ifPresent(extraActions::add);
      if (activeAndDraftQuestions.getActiveQuestionDefinition(question.getName()).isPresent()) {
        extraActions.add(renderDiscardDraftLink(question, request));
      }
    }
    // Add Archive option only if current question is draft or it's active, but there is no
    // draft version of the question.
    if (isEditable) {
      Pair<DomContent, Optional<Modal>> archiveOptionsAndModal =
          renderArchiveOptions(question, activeAndDraftQuestions, request);
      extraActions.add(archiveOptionsAndModal.getLeft());
      archiveOptionsAndModal.getRight().ifPresent(modals::add);
    }

    // Build extra actions button and menu.
    String extraActionsButtonId = "extra-actions-" + Modal.randomModalId();
    ButtonTag extraActionsButton =
        makeSvgTextButton("", Icons.MORE_VERT)
            .withId(extraActionsButtonId)
            .withClasses(
                AdminStyles.TERTIARY_BUTTON_STYLES,
                ReferenceClasses.WITH_DROPDOWN,
                Styles.H_12,
                extraActions.build().isEmpty() ? Styles.INVISIBLE : "");

    Pair<ButtonTag, Optional<Modal>> editButtonAndModal = 
      getEditButtonAndModal(activeAndDraftQuestions, question, isEditable);
    ButtonTag editButton = editButtonAndModal.getLeft();
    logger.error("editButton: " + editButtonAndModal.getRight().isPresent());
    if (editButtonAndModal.getRight().isPresent()) {
      modals.add(editButtonAndModal.getRight().get());
    }
    DivTag result =
        div()
            .withClasses(Styles.FLEX, Styles.SPACE_X_2, Styles.PR_6, Styles.FONT_MEDIUM)
            .with(editButton)
            .with(
                div()
                    .withClass(Styles.RELATIVE)
                    .with(
                        extraActionsButton,
                        div()
                            .withId(extraActionsButtonId + "-dropdown")
                            .withClasses(
                                Styles.HIDDEN,
                                Styles.FLEX,
                                Styles.FLEX_COL,
                                Styles.BORDER,
                                Styles.BG_WHITE,
                                Styles.ABSOLUTE,
                                Styles.RIGHT_0,
                                Styles.W_56,
                                Styles.Z_50)
                            .with(extraActions.build())));

    return Pair.of(result, modals.build());
  }

  private ButtonTag renderDiscardDraftLink(QuestionDefinition definition, Http.Request request) {
    String link =
        controllers.admin.routes.AdminQuestionController.discardDraft(definition.getId()).url();
    return toLinkButtonForPost(
        makeSvgTextButton("Discard Draft", Icons.DELETE)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
        link,
        request);
  }

  private Pair<DomContent, Optional<Modal>> renderArchiveOptions(
      QuestionDefinition definition,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    switch (activeAndDraftQuestions.getDeletionStatus(definition.getName())) {
      case PENDING_DELETION:
        String restoreLink =
            controllers.admin.routes.AdminQuestionController.restore(definition.getId()).url();
        ButtonTag unarchiveButton =
            toLinkButtonForPost(
                makeSvgTextButton("Restore Archived", Icons.UNARCHIVE)
                    .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
                restoreLink,
                request);
        return Pair.of(unarchiveButton, Optional.empty());
      case DELETABLE:
        String archiveLink =
            controllers.admin.routes.AdminQuestionController.archive(definition.getId()).url();
        ButtonTag archiveButton =
            toLinkButtonForPost(
                makeSvgTextButton("Archive", Icons.ARCHIVE)
                    .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
                archiveLink,
                request);
        return Pair.of(archiveButton, Optional.empty());
      default:
        DivTag modalHeader =
            div()
                .withClasses(
                    Styles.P_2,
                    Styles.BORDER,
                    Styles.BORDER_GRAY_400,
                    Styles.BG_GRAY_200,
                    Styles.TEXT_SM)
                .with(
                    span(
                        "This question cannot be archived since there are still programs"
                            + " using it. Please remove all usages from the below"
                            + " programs before attempting to archive."));

        ActiveAndDraftQuestions.ReferencingPrograms programs =
            activeAndDraftQuestions.getReferencingPrograms(definition.getName());
        GroupedReferencingPrograms referencingPrograms =
            createReferencingPrograms(programs.activeReferences(), programs.draftReferences());
        Optional<Modal> maybeModal =
            makeReferencingProgramsModal(
                definition.getName(), referencingPrograms, Optional.of(modalHeader));
        ButtonTag cantArchiveButton =
            makeSvgTextButton("Archive", Icons.ARCHIVE)
                .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES)
                .withId(maybeModal.get().getTriggerButtonId());

        return Pair.of(cantArchiveButton, maybeModal);
    }
  }
}
