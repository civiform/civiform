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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.SpanTag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.CreateQuestionButton;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Renders a page for viewing all active questions and draft questions.
 */
public final class QuestionsListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final TranslationLocales translationLocales;

  @Inject
  public QuestionsListView(
    AdminLayoutFactory layoutFactory, TranslationLocales translationLocales) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    this.translationLocales = checkNotNull(translationLocales);
  }

  /**
   * Renders a page with a table view of all questions.
   */
  public Content render(ActiveAndDraftQuestions activeAndDraftQuestions,
    Http.Request request) {
    String title = "All Questions";

    Pair<ImmutableList<DomContent>, ImmutableList<Modal>> questionRowsAndModals =
      renderAllQuestionRows(activeAndDraftQuestions, request);

    DivTag contentDiv =
      div()
        .withClasses(Styles.PX_4)
        .with(
          div()
            .withClasses(
              Styles.FLEX,
              Styles.ITEMS_CENTER,
              Styles.SPACE_X_4,
              Styles.MT_12,
              Styles.MB_10)
            .with(
              h1(title),
              div().withClass(Styles.FLEX_GROW),
              CreateQuestionButton.renderCreateQuestionButton(
                controllers.admin.routes.AdminQuestionController.index()
                  .url())))
        .with(questionRowsAndModals.getLeft())
        .with(
          renderSummary(activeAndDraftQuestions)
        );
    HtmlBundle htmlBundle =
      layout
        .getBundle()
        .setTitle(title)
        .addModals(questionRowsAndModals.getRight())
        .addMainContent(
          contentDiv);

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

  private DivTag renderSummary(
    ActiveAndDraftQuestions activeAndDraftQuestions) {
    // The total question count should be equivalent to the number of rows in the displayed table,
    // where we have a single entry for a question that is active and has a draft.
    return div(String.format(
      "Total Questions: %d", activeAndDraftQuestions.getQuestionNames().size()))
      .withClasses(Styles.FLOAT_RIGHT, Styles.TEXT_BASE, Styles.PX_4,
        Styles.MY_2);
  }

  /**
   * Renders the full table.
   */
  private Pair<ImmutableList<DomContent>, ImmutableList<Modal>> renderAllQuestionRows(
    ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    ImmutableList.Builder<DomContent> rows = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    for (String name : activeAndDraftQuestions.getQuestionNames()) {
      Pair<DivTag, ImmutableList<Modal>> rowAndModals = renderQuestionRow(name, activeAndDraftQuestions, request);
      rows.add(rowAndModals.getLeft());
      modals.addAll(rowAndModals.getRight());
    }
    return Pair.of(rows.build(), modals.build());
  }

  /** Render the question table header row. */
  // private TheadTag renderQuestionTableHeaderRow() {
  //   return thead(
  //       tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
  //           .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_12))
  //           .with(th("Question text").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_3_12))
  //           .with(
  //               th("Supported languages").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_12))
  //           .with(
  //               th("Referencing programs").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_12))
  //           .with(
  //               th("Actions")
  //                   .withClasses(
  //                       BaseStyles.TABLE_CELL_STYLES,
  //                       Styles.PL_4,
  //                       Styles.W_2_12)));
  // }

  /**
   * Display this as a table row with all fields.
   *
   * <p>One of {@code activeDefinition} and {@code draftDefinition} must be
   * specified.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderQuestionRow(
    String questionName, ActiveAndDraftQuestions activeAndDraftQuestions,
    Http.Request request) {
    Optional<QuestionDefinition> activeDefinition =
      activeAndDraftQuestions.getActiveQuestionDefinition(questionName);
    Optional<QuestionDefinition> draftDefinition =
      activeAndDraftQuestions.getDraftQuestionDefinition(questionName);
    if (draftDefinition.isEmpty() && activeDefinition.isEmpty()) {
      throw new IllegalArgumentException("Did not receive a valid question.");
    }
    QuestionDefinition latestDefinition = draftDefinition.orElseGet(
      activeDefinition::get);

    DivTag row =
      div().withClasses(
          Styles.FLEX)
        .with(renderInfoCell(latestDefinition));
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();

    DivTag draftAndActiveRows = div()
      .withClasses(
        Styles.FLEX_GROW
      );
    if (draftDefinition.isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> draftRow = renderActiveOrDraftRow(/* isActive= */
        false, draftDefinition.get(), activeAndDraftQuestions, request);
      modals.addAll(draftRow.getRight());
      draftAndActiveRows.with(draftRow.getLeft());
    }
    if (activeDefinition.isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> activeRow = renderActiveOrDraftRow(/* isActive= */
        true, activeDefinition.get(), activeAndDraftQuestions, request);
      modals.addAll(activeRow.getRight());
      draftAndActiveRows.with(activeRow.getLeft());
    }
    row.with(draftAndActiveRows);

    DivTag adminNote = div()
      .withClasses(Styles.PY_7)
      .with(span("Admin note: ").withClasses(Styles.FONT_BOLD))
      .with(span(latestDefinition.getName()), br(), span(latestDefinition.getDescription()));

    DivTag rowWithAdminNote = div()
      .withClasses(
        Styles.W_FULL,
        Styles.MY_4, Styles.PL_6, Styles.BORDER_GRAY_300, Styles.ROUNDED_LG, Styles.BORDER,
        ReferenceClasses.ADMIN_QUESTION_TABLE_ROW)
      .with(row)
        .with(adminNote);
    return Pair.of(rowWithAdminNote, modals.build());
  }

  private Pair<DivTag, ImmutableList<Modal>> renderActiveOrDraftRow(
    boolean isActive, QuestionDefinition question,
    ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {

    String badgeText = "Draft";
    String badgeBGColor = BaseStyles.BG_CIVIFORM_PURPLE_LIGHT;
    String badgeFillColor = BaseStyles.TEXT_CIVIFORM_PURPLE;
    if (isActive) {
      badgeText = "Active";
      badgeBGColor = BaseStyles.BG_CIVIFORM_GREEN_LIGHT;
      badgeFillColor = BaseStyles.TEXT_CIVIFORM_GREEN;
    }
    boolean isSecondRow = isActive && activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isPresent();

    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
      activeAndDraftQuestions.getReferencingPrograms(question.getId());
    Pair<DivTag, ImmutableList<Modal>> referencingProgramAndModal =
      renderPublishedDateAndReferencingPrograms(question.getName(),
        activeAndDraftQuestions, referencingPrograms.activeReferences(),
        referencingPrograms.draftReferences());
    Pair<DivTag, ImmutableList<Modal>> actionsCellAndModal =
      renderActionsCell(isActive, question, activeAndDraftQuestions, request);

    PTag badge =
      p().withClasses(
          badgeBGColor,
          badgeFillColor,
          Styles.ML_2,
          StyleUtils.responsiveXLarge(Styles.ML_8),
          Styles.FONT_MEDIUM,
          Styles.ROUNDED_FULL,
          Styles.FLEX,
          Styles.FLEX_ROW,
          Styles.GAP_X_2,
          Styles.PLACE_ITEMS_CENTER,
          Styles.JUSTIFY_CENTER,
          Styles.H_12)
        .withStyle("width: 100px")
        .with(
          Icons.svg(Icons.NOISE_CONTROL_OFF)
            .withClasses(Styles.INLINE_BLOCK, Styles.ML_3_5),
          span(badgeText).withClass(Styles.MR_4));

    DivTag row = div()
      .withClasses(
        Styles.PY_7,
        Styles.FLEX,
        Styles.FLEX_ROW,
        Styles.ITEMS_CENTER,
        StyleUtils.hover(Styles.BG_GRAY_100),
        Styles.CURSOR_POINTER,
        isSecondRow ? Styles.BORDER_T : "")
      .with(badge)
      .with(referencingProgramAndModal.getLeft())
      .with(div().withClasses(Styles.FLEX_GROW))
      .with(actionsCellAndModal.getLeft());

    asRedirectElement(row,
      controllers.admin.routes.AdminQuestionController.show(question.getId())
        .url());

    return Pair.of(row,
      ImmutableList.<Modal>builder()
        .addAll(referencingProgramAndModal.getRight())
        .addAll(actionsCellAndModal.getRight())
        .build());
  }

  private DivTag renderInfoCell(QuestionDefinition definition) {
    DivTag questionText = div().withClasses(Styles.FONT_BOLD, Styles.TEXT_BLACK,
        Styles.FLEX, Styles.FLEX_ROW, Styles.ITEMS_CENTER)
      .with(Icons.questionTypeSvg(definition.getQuestionType())
        .withClasses(Styles.W_6, Styles.H_6, Styles.FLEX_SHRINK_0))
      .with(
        div(definition.getQuestionText().getDefault()).withClasses(Styles.PL_4,
          Styles.TEXT_XL));
    DivTag questionDescription = div(
      div(definition.getQuestionHelpText().isEmpty() ? ""
        : definition.getQuestionHelpText().getDefault()).withClasses(
        Styles.PL_10));
    return div()
      .withClasses(Styles.PY_7, Styles.W_1_4, Styles.FLEX, Styles.FLEX_COL, Styles.JUSTIFY_BETWEEN)
      .with(div()
        .with(questionText)
        .with(questionDescription));
  }

  private Pair<DivTag, ImmutableList<Modal>> renderPublishedDateAndReferencingPrograms(
    String questionName, ActiveAndDraftQuestions activeAndDraftQuestions,
    Collection<ProgramDefinition> activePrograms,
    Collection<ProgramDefinition> draftPrograms) {

    Optional<Modal> maybeReferencingProgramsModal =
      makeReferencingProgramsModal(
        questionName, activePrograms, draftPrograms, /* modalHeader= */
        Optional.empty());

    ArrayList<String> parts = new ArrayList<>();
    if (!activePrograms.isEmpty()) {
      parts.add(String.format("%d active", activePrograms.size()));
    }
    if (activeAndDraftQuestions.draftVersionHasAnyEdits() && !draftPrograms.isEmpty()) {
      parts.add(String.format("%d draft", draftPrograms.size()));
    }
    if (parts.isEmpty()) {
      parts.add("0");
    }
    SpanTag referencingProgramsCount =
      span(Joiner.on(" & ").join(parts))
        .with(span(" programs"))
        .withClass(Styles.FONT_SEMIBOLD);

    DivTag tag =
      div()
        .withClasses(
          ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS,
          Styles.ML_4, StyleUtils.responsiveXLarge(Styles.ML_10))
        .with(span("Used across "), referencingProgramsCount);
    if (maybeReferencingProgramsModal.isPresent()) {
      tag.with(
        span(" "),
        a().withId(maybeReferencingProgramsModal.get().getTriggerButtonId())
          .withClasses(
            Styles.CURSOR_POINTER,
            Styles.FONT_SEMIBOLD,
            Styles.UNDERLINE,
            BaseStyles.TEXT_SEATTLE_BLUE,
            StyleUtils.hover(Styles.TEXT_BLACK))
          .withText("See list"));
    }
    return Pair.of(tag, maybeReferencingProgramsModal.map(ImmutableList::of)
      .orElse(ImmutableList.of()));
  }

  private Optional<Modal> makeReferencingProgramsModal(
    String questionName,
    Collection<ProgramDefinition> activePrograms,
    Collection<ProgramDefinition> draftPrograms,
    Optional<DomContent> modalHeader) {
    ImmutableList<ProgramDefinition> activeProgramReferences =
      activePrograms.stream()
        .sorted(Comparator.comparing(ProgramDefinition::adminName))
        .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> draftProgramReferences =
      draftPrograms.stream()
        .sorted(Comparator.comparing(ProgramDefinition::adminName))
        .collect(ImmutableList.toImmutableList());

    if (activeProgramReferences.isEmpty() && draftProgramReferences.isEmpty()) {
      return Optional.empty();
    }

    DivTag referencingProgramModalContent =
      div().withClasses(Styles.P_6, Styles.FLEX_ROW, Styles.SPACE_Y_6);
    if (modalHeader.isPresent()) {
      referencingProgramModalContent.with(modalHeader.get());
    }
    referencingProgramModalContent.with(
      div()
        .withClass(
          ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_ACTIVE)
        .with(
          referencingProgramList("Active programs:", activePrograms)),
      div()
        .withClass(
          ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_DRAFT)
        .with(referencingProgramList("Draft programs:", draftPrograms)),
      p("Note: This list does not automatically refresh. If edits are made to a program"
        + " in a separate tab, they won't be reflected until the page has been"
        + " refreshed.")
        .withClass(Styles.TEXT_SM));
    return Optional.of(
      Modal.builder(Modal.randomModalId(), referencingProgramModalContent)
        .setModalTitle(String.format("Programs including %s", questionName))
        .setWidth(Width.HALF)
        .build());
  }

  private DivTag referencingProgramList(
    String title, Collection<ProgramDefinition> referencingPrograms) {
    // TODO(#3162): Add ability to view a published program. Then add
    // links to the specific block that references the question.
    ImmutableList<ProgramDefinition> sortedReferencingPrograms =
      referencingPrograms.stream()
        .sorted(Comparator.comparing(ProgramDefinition::adminName))
        .collect(ImmutableList.toImmutableList());
    return div()
      .with(p(title).withClass(Styles.FONT_SEMIBOLD))
      .condWith(sortedReferencingPrograms.isEmpty(),
        p("None").withClass(Styles.PL_5))
      .condWith(
        !sortedReferencingPrograms.isEmpty(),
        div()
          .with(
            ul().withClasses(Styles.LIST_DISC, Styles.LIST_INSIDE)
              .with(
                each(
                  sortedReferencingPrograms,
                  programReference -> {
                    return li(programReference.adminName());
                  }))));
  }

  private ButtonTag renderQuestionEditLink(QuestionDefinition definition, boolean isVisible) {
    String link = controllers.admin.routes.AdminQuestionController.edit(
      definition.getId()).url();
    return asRedirectElement(
      makeSvgTextButton("Edit", Icons.EDIT).withClasses(
        AdminStyles.TERTIARY_BUTTON_STYLES, isVisible ? "" : Styles.INVISIBLE),
      link);
  }

  private Optional<ButtonTag> renderQuestionTranslationLink(
    QuestionDefinition definition) {
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
    boolean isEditable = !isActive || activeAndDraftQuestions.getDraftQuestionDefinition(
      question.getName()).isEmpty();

    if (!isActive) {
      Optional<ButtonTag> maybeTranslationLink =
        renderQuestionTranslationLink(question);
      maybeTranslationLink.ifPresent(extraActions::add);
      if (activeAndDraftQuestions.getActiveQuestionDefinition(
        question.getName()).isPresent()) {
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
    DivTag result = div()
      .withClasses(Styles.FLEX, Styles.SPACE_X_2, Styles.PR_6,
        Styles.FONT_MEDIUM)
      .with(renderQuestionEditLink(question, isEditable))
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

  private ButtonTag renderDiscardDraftLink(QuestionDefinition definition,
    Http.Request request) {
    String link =
      controllers.admin.routes.AdminQuestionController.discardDraft(
        definition.getId()).url();
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
          controllers.admin.routes.AdminQuestionController.restore(
            definition.getId()).url();
        ButtonTag unarchiveButton = toLinkButtonForPost(
          makeSvgTextButton("Restore Archived", Icons.UNARCHIVE)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES), restoreLink,
          request);
        return Pair.of(
          unarchiveButton, Optional.empty());
      case DELETABLE:
        String archiveLink =
          controllers.admin.routes.AdminQuestionController.archive(
            definition.getId()).url();
        ButtonTag archiveButton = toLinkButtonForPost(
          makeSvgTextButton("Archive", Icons.ARCHIVE)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES), archiveLink,
          request);
        return Pair.of(
          archiveButton,
          Optional.empty());
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
                  + " referencing  it. Please remove all references from the below"
                  + " programs before attempting to archive."));

        ActiveAndDraftQuestions.ReferencingPrograms programs = activeAndDraftQuestions.getReferencingPrograms(
          definition.getId());
        Optional<Modal> maybeModal =
          makeReferencingProgramsModal(
            definition.getName(),
            programs.activeReferences(), programs.draftReferences(),
            Optional.of(modalHeader));
        ButtonTag cantArchiveButton = makeSvgTextButton("Archive",
          Icons.ARCHIVE)
          .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES)
          .withId(maybeModal.get().getTriggerButtonId());

        return Pair.of(
          cantArchiveButton,
          maybeModal);
    }
  }
}
