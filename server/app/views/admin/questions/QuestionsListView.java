package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;
import static j2html.TagCreator.each;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.SpanTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeletionStatus;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for viewing all active questions and draft questions. */
public final class QuestionsListView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionsListView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
  }

  /** Renders a page with a table view of all questions. */
  public Content render(
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Optional<String> maybeFlash,
      Http.Request request) {
    String title = "All Questions";

    Pair<TableTag, ImmutableList<Modal>> questionTableAndModals =
        renderQuestionTable(activeAndDraftQuestions, request);

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addModals(questionTableAndModals.getRight())
            .addMainContent(
                renderHeader(title),
                renderAddQuestionLink(),
                div(questionTableAndModals.getLeft()).withClasses(Styles.M_4),
                renderSummary(activeAndDraftQuestions));

    if (maybeFlash.isPresent()) {
      // Right now, we only show success messages when this page is rendered with maybeFlash set,
      // so we use the success ToastMessage type by default.
      htmlBundle.addToastMessages(ToastMessage.success(maybeFlash.get()).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderAddQuestionLink() {
    String parentId = "create-question-button";
    String dropdownId = parentId + "-dropdown";
    DivTag linkButton =
        new LinkElement().setId(parentId).setText("Create new question").asButtonNoHref();
    DivTag dropdown =
        div()
            .withId(dropdownId)
            .withClasses(
                Styles.BORDER,
                Styles.BG_WHITE,
                Styles.TEXT_GRAY_600,
                Styles.SHADOW_LG,
                Styles.ABSOLUTE,
                Styles.MT_3,
                Styles.HIDDEN);

    for (QuestionType type : QuestionType.values()) {
      String typeString = type.toString().toLowerCase();
      String link = controllers.admin.routes.AdminQuestionController.newOne(typeString).url();
      ATag linkTag =
          a().withHref(link)
              .withId(String.format("create-%s-question", typeString))
              .withClasses(
                  Styles.BLOCK,
                  Styles.P_3,
                  Styles.BG_WHITE,
                  Styles.TEXT_GRAY_600,
                  StyleUtils.hover(Styles.BG_GRAY_100, Styles.TEXT_GRAY_800))
              .with(
                  Icons.questionTypeSvg(type, 24)
                      .withClasses(
                          Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6, Styles.MR_1, Styles.TEXT_SM))
              .with(
                  p(type.getLabel())
                      .withClasses(
                          Styles.ML_2,
                          Styles.MR_4,
                          Styles.INLINE,
                          Styles.TEXT_SM,
                          Styles.UPPERCASE));
      dropdown.with(linkTag);
    }
    return linkButton.with(dropdown);
  }

  private DivTag renderSummary(ActiveAndDraftQuestions activeAndDraftQuestions) {
    // The total question count should be equivalent to the number of rows in the displayed table,
    // where we have a single entry for a question that is active and has a draft.
    return div(String.format(
            "Total Questions: %d", activeAndDraftQuestions.getQuestionNames().size()))
        .withClasses(Styles.FLOAT_RIGHT, Styles.TEXT_BASE, Styles.PX_4, Styles.MY_2);
  }

  /** Renders the full table. */
  private Pair<TableTag, ImmutableList<Modal>> renderQuestionTable(
      ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    ImmutableList<Pair<TrTag, Optional<Modal>>> tableRowAndModals =
        activeAndDraftQuestions.getQuestionNames().stream()
            .map(
                (questionName) ->
                    renderQuestionTableRow(questionName, activeAndDraftQuestions, request))
            .collect(ImmutableList.toImmutableList());
    TableTag tableTag =
        table()
            .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
            .with(renderQuestionTableHeaderRow())
            .with(tbody(each(tableRowAndModals, (tableRowAndModal) -> tableRowAndModal.getLeft())));
    ImmutableList<Modal> modals =
        tableRowAndModals.stream()
            .map(Pair::getRight)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(ImmutableList.toImmutableList());
    return Pair.of(tableTag, modals);
  }

  /** Render the question table header row. */
  private TheadTag renderQuestionTableHeaderRow() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_12))
            .with(th("Question text").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_3_12))
            .with(
                th("Supported languages").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_12))
            .with(
                th("Referencing programs").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_12))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES,
                        Styles.TEXT_RIGHT,
                        Styles.PR_8,
                        Styles.W_2_12)));
  }

  /**
   * Display this as a table row with all fields.
   *
   * <p>One of {@code activeDefinition} and {@code draftDefinition} must be specified.
   */
  private Pair<TrTag, Optional<Modal>> renderQuestionTableRow(
      String questionName, ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    Optional<QuestionDefinition> activeDefinition =
        activeAndDraftQuestions.getActiveQuestionDefinition(questionName);
    Optional<QuestionDefinition> draftDefinition =
        activeAndDraftQuestions.getDraftQuestionDefinition(questionName);
    DeletionStatus deletionStatus = activeAndDraftQuestions.getDeletionStatus(questionName);
    if (draftDefinition.isEmpty() && activeDefinition.isEmpty()) {
      throw new IllegalArgumentException("Did not receive a valid question.");
    }
    QuestionDefinition latestDefinition = draftDefinition.orElseGet(() -> activeDefinition.get());
    Pair<TdTag, Optional<Modal>> referencingProgramAndModal =
        renderReferencingPrograms(questionName, activeAndDraftQuestions);
    TrTag rowTag =
        tr().withClasses(
                ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
                Styles.BORDER_B,
                Styles.BORDER_GRAY_300,
                StyleUtils.even(Styles.BG_GRAY_100))
            .with(renderInfoCell(latestDefinition))
            .with(renderQuestionTextCell(latestDefinition))
            .with(renderSupportedLanguages(latestDefinition))
            .with(referencingProgramAndModal.getLeft())
            .with(renderActionsCell(activeDefinition, draftDefinition, deletionStatus, request));
    return Pair.of(rowTag, referencingProgramAndModal.getRight());
  }

  private TdTag renderInfoCell(QuestionDefinition definition) {
    return td().with(div(definition.getName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(definition.getDescription()).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderQuestionTextCell(QuestionDefinition definition) {
    String questionText = "";
    String questionHelpText = "";

    try {
      questionText = definition.getQuestionText().get(LocalizedStrings.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    try {
      questionHelpText = definition.getQuestionHelpText().get(LocalizedStrings.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    return td().with(div(questionText).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(questionHelpText).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  /**
   * Render the supported languages for this question in US English (ex: "es-US" will appear as
   * "Spanish").
   */
  private TdTag renderSupportedLanguages(QuestionDefinition definition) {
    String formattedLanguages =
        definition.getSupportedLocales().stream()
            .map(locale -> locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE))
            .collect(Collectors.joining(", "));
    return td().with(div(formattedLanguages)).withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private Pair<TdTag, Optional<Modal>> renderReferencingPrograms(
      String questionName, ActiveAndDraftQuestions activeAndDraftQuestions) {
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        activeAndDraftQuestions.getReferencingPrograms(questionName);

    Optional<Modal> maybeReferencingProgramsModal =
        makeReferencingProgramsModal(questionName, referencingPrograms);

    SpanTag referencingProgramsCount =
        span(String.format("%d active", referencingPrograms.activeReferences().size()))
            .condWith(
                referencingPrograms.draftReferences().isPresent(),
                span(
                    String.format(
                        " & %d draft",
                        referencingPrograms.draftReferences().map(ImmutableSet::size).orElse(0))))
            .with(span(" programs"))
            .withClass(Styles.FONT_SEMIBOLD);

    ContainerTag referencingProgramsCountContainer = referencingProgramsCount;
    if (maybeReferencingProgramsModal.isPresent()) {
      referencingProgramsCountContainer =
          a().withId(maybeReferencingProgramsModal.get().getTriggerButtonId())
              .withClasses(Styles.DECORATION_SOLID, Styles.CURSOR_POINTER)
              .with(referencingProgramsCount);
    }

    TdTag tag =
        td().with(p().with(span("Used across "), referencingProgramsCountContainer))
            .withClasses(
                ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS,
                BaseStyles.TABLE_CELL_STYLES);
    return Pair.of(tag, maybeReferencingProgramsModal);
  }

  private Optional<Modal> makeReferencingProgramsModal(
      String questionName, ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms) {
    ImmutableList<ProgramDefinition> activeProgramReferences =
        referencingPrograms.activeReferences().stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> draftProgramReferences =
        referencingPrograms.draftReferences().orElse(ImmutableSet.of()).stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());

    if (activeProgramReferences.isEmpty() && draftProgramReferences.isEmpty()) {
      return Optional.empty();
    }

    DivTag referencingProgramModalContent =
        div()
            .withClasses(Styles.P_6, Styles.FLEX_ROW, Styles.SPACE_Y_6)
            .with(
                div()
                    .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_ACTIVE)
                    .with(
                        referencingProgramList(
                            "Active programs:", referencingPrograms.activeReferences())),
                div()
                    .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_DRAFT)
                    .with(
                        referencingProgramList(
                            "Draft programs:",
                            referencingPrograms.draftReferences().orElse(ImmutableSet.of()))),
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
      String title, ImmutableSet<ProgramDefinition> referencingPrograms) {
    ImmutableList<ProgramDefinition> sortedReferencingPrograms =
        referencingPrograms.stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    return div()
        .with(p(title).withClass(Styles.FONT_SEMIBOLD))
        .condWith(sortedReferencingPrograms.isEmpty(), p("None").withClass(Styles.PL_5))
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

  private ATag renderQuestionEditLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.AdminQuestionController.edit(definition.getId()).url();
    return new LinkElement()
        .setId("edit-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }

  private ATag renderQuestionTranslationLink(QuestionDefinition definition, String linkText) {
    String link =
        controllers.admin.routes.AdminQuestionTranslationsController.edit(
                definition.getId(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    return new LinkElement()
        .setId("translate-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }

  private ATag renderQuestionViewLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.AdminQuestionController.show(definition.getId()).url();
    return new LinkElement()
        .setId("view-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }

  private TdTag renderActionsCell(
      Optional<QuestionDefinition> active,
      Optional<QuestionDefinition> draft,
      DeletionStatus deletionStatus,
      Http.Request request) {
    TdTag td = td().withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.TEXT_RIGHT);
    if (active.isPresent()) {
      if (draft.isEmpty()) {
        // Active without a draft.
        td.with(renderQuestionViewLink(active.get(), "View →")).with(br());
        td.with(renderQuestionEditLink(active.get(), "New Version →")).with(br());
      } else if (draft.isPresent()) {
        // Active with a draft.
        td.with(renderQuestionViewLink(active.get(), "View Published →")).with(br());
        td.with(renderQuestionEditLink(draft.get(), "Edit Draft →")).with(br());
        td.with(renderQuestionTranslationLink(draft.get(), "Manage Draft Translations →"))
            .with(br());
        td.with(renderDiscardDraftLink(draft.get(), "Discard Draft →", request)).with(br());
      }
    } else if (draft.isPresent()) {
      // First revision of a question.
      td.with(renderQuestionEditLink(draft.get(), "Edit Draft →")).with(br());
      td.with(renderQuestionTranslationLink(draft.get(), "Manage Translations →")).with(br());
    }
    // Add Archive options.
    if (active.isPresent()) {
      if (deletionStatus.equals(DeletionStatus.PENDING_DELETION)) {
        td.with(renderRestoreQuestionLink(active.get(), "Restore Archived →", request)).with(br());
      } else if (deletionStatus.equals(DeletionStatus.DELETABLE)) {
        td.with(renderArchiveQuestionLink(active.get(), "Archive →", request)).with(br());
      }
    }
    return td;
  }

  private FormTag renderDiscardDraftLink(
      QuestionDefinition definition, String linkText, Http.Request request) {
    String link =
        controllers.admin.routes.AdminQuestionController.discardDraft(definition.getId()).url();
    return new LinkElement()
        .setId("discard-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asHiddenFormLink(request);
  }

  private FormTag renderRestoreQuestionLink(
      QuestionDefinition definition, String linkText, Http.Request request) {
    String link =
        controllers.admin.routes.AdminQuestionController.restore(definition.getId()).url();
    return new LinkElement()
        .setId("restore-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asHiddenFormLink(request);
  }

  private FormTag renderArchiveQuestionLink(
      QuestionDefinition definition, String linkText, Http.Request request) {
    String link =
        controllers.admin.routes.AdminQuestionController.archive(definition.getId()).url();
    return new LinkElement()
        .setId("archive-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asHiddenFormLink(request);
  }
}
