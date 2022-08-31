package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
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
import services.LocalizedStrings;
import services.TranslationLocales;
import services.TranslationNotFoundException;
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

/** Renders a page for viewing all active questions and draft questions. */
public final class QuestionsListView extends BaseHtmlView {
  private final AdminLayout layout;
  private final TranslationLocales translationLocales;

  @Inject
  public QuestionsListView(
      AdminLayoutFactory layoutFactory, TranslationLocales translationLocales) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    this.translationLocales = checkNotNull(translationLocales);
  }

  /** Renders a page with a table view of all questions. */
  public Content render(ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
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
                CreateQuestionButton.renderCreateQuestionButton(
                    controllers.admin.routes.AdminQuestionController.index().url()),
                div(questionTableAndModals.getLeft()).withClasses(Styles.M_4),
                renderSummary(activeAndDraftQuestions));

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

  /** Renders the full table. */
  private Pair<TableTag, ImmutableList<Modal>> renderQuestionTable(
      ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    ImmutableList<Pair<TrTag, ImmutableList<Modal>>> tableRowAndModals =
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
            .flatMap(ImmutableList::stream)
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
                    .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PL_8, Styles.W_2_12)));
  }

  /**
   * Display this as a table row with all fields.
   *
   * <p>One of {@code activeDefinition} and {@code draftDefinition} must be specified.
   */
  private Pair<TrTag, ImmutableList<Modal>> renderQuestionTableRow(
      String questionName, ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    Optional<QuestionDefinition> activeDefinition =
        activeAndDraftQuestions.getActiveQuestionDefinition(questionName);
    Optional<QuestionDefinition> draftDefinition =
        activeAndDraftQuestions.getDraftQuestionDefinition(questionName);
    if (draftDefinition.isEmpty() && activeDefinition.isEmpty()) {
      throw new IllegalArgumentException("Did not receive a valid question.");
    }
    QuestionDefinition latestDefinition = draftDefinition.orElseGet(() -> activeDefinition.get());

    Pair<TdTag, Optional<Modal>> referencingProgramAndModal =
        renderReferencingPrograms(questionName, activeAndDraftQuestions);
    Pair<TdTag, Optional<Modal>> actionsCellAndModal =
        renderActionsCell(activeDefinition, draftDefinition, activeAndDraftQuestions, request);
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
            .with(actionsCellAndModal.getLeft());
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    if (referencingProgramAndModal.getRight().isPresent()) {
      modals.add(referencingProgramAndModal.getRight().get());
    }
    if (actionsCellAndModal.getRight().isPresent()) {
      modals.add(actionsCellAndModal.getRight().get());
    }
    return Pair.of(rowTag, modals.build());
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
        makeReferencingProgramsModal(
            questionName, referencingPrograms, /* modalHeader= */ Optional.empty());

    SpanTag referencingProgramsCount =
        span(String.format("%d active", referencingPrograms.activeReferences().size()))
            .condWith(
                activeAndDraftQuestions.draftVersionHasAnyEdits(),
                span(String.format(" & %d draft", referencingPrograms.draftReferences().size())))
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
      String questionName,
      ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms,
      Optional<DomContent> modalHeader) {
    ImmutableList<ProgramDefinition> activeProgramReferences =
        referencingPrograms.activeReferences().stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> draftProgramReferences =
        referencingPrograms.draftReferences().stream()
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
            .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_ACTIVE)
            .with(
                referencingProgramList("Active programs:", referencingPrograms.activeReferences())),
        div()
            .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_DRAFT)
            .with(referencingProgramList("Draft programs:", referencingPrograms.draftReferences())),
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
    // TODO(#3162): Add ability to view a published program. Then add
    // links to the specific block that references the question.
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

  private ButtonTag renderQuestionEditLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.AdminQuestionController.edit(definition.getId()).url();
    return asRedirectElement(
        makeSvgTextButton(linkText, Icons.EDIT).withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
        link);
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

  private ButtonTag renderQuestionViewLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.AdminQuestionController.show(definition.getId()).url();
    return asRedirectElement(
        makeSvgTextButton(linkText, Icons.VISIBILITY)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
        link);
  }

  private Pair<TdTag, Optional<Modal>> renderActionsCell(
      Optional<QuestionDefinition> active,
      Optional<QuestionDefinition> draft,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    TdTag td = td().withClasses(BaseStyles.TABLE_CELL_STYLES);
    if (active.isPresent()) {
      if (draft.isEmpty()) {
        // Active without a draft.
        td.with(renderQuestionViewLink(active.get(), "View"));
        td.with(renderQuestionEditLink(active.get(), "New Version"));
      } else if (draft.isPresent()) {
        // Active with a draft.
        td.with(renderQuestionViewLink(active.get(), "View Published"));
        td.with(renderQuestionEditLink(draft.get(), "Edit Draft"));
        Optional<ButtonTag> maybeTranslationLink = renderQuestionTranslationLink(draft.get());
        if (maybeTranslationLink.isPresent()) {
          td.with(maybeTranslationLink.get());
        }
        td.with(renderDiscardDraftLink(draft.get(), request));
      }
    } else if (draft.isPresent()) {
      // First revision of a question.
      td.with(renderQuestionEditLink(draft.get(), "Edit Draft"));
      Optional<ButtonTag> maybeTranslationLink = renderQuestionTranslationLink(draft.get());
      if (maybeTranslationLink.isPresent()) {
        td.with(maybeTranslationLink.get());
      }
    }
    // Add Archive options.
    QuestionDefinition questionForArchive = draft.isPresent() ? draft.get() : active.get();
    Pair<DomContent, Optional<Modal>> archiveOptionsAndModal =
        renderArchiveOptions(questionForArchive, activeAndDraftQuestions, request);
    DomContent archiveOptions = archiveOptionsAndModal.getLeft();
    Optional<Modal> maybeArchiveModal = archiveOptionsAndModal.getRight();
    td.with(archiveOptions);
    return Pair.of(td, maybeArchiveModal);
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
                            + " referencing  it. Please remove all references from the below"
                            + " programs before attempting to archive."));

        Optional<Modal> maybeModal =
            makeReferencingProgramsModal(
                definition.getName(),
                activeAndDraftQuestions.getReferencingPrograms(definition.getName()),
                Optional.of(modalHeader));

        ButtonTag cantArchiveButton =
            makeSvgTextButton("Archive", Icons.ARCHIVE)
                .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES)
                .withId(maybeModal.get().getTriggerButtonId());

        return Pair.of(cantArchiveButton, maybeModal);
    }
  }
}
