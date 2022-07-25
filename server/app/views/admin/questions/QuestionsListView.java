package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;

import com.google.inject.Inject;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.Optional;
import java.util.stream.Collectors;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeletionStatus;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
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
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;


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

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                renderHeader(title),
                renderAddQuestionLink(),
                div(renderQuestionTable(activeAndDraftQuestions, request)).withClasses("m-4"),
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
                "border",
                "bg-white",
                "text-gray-600",
                "shadow-lg",
                "absolute",
                "mt-3",
                "hidden");

    for (QuestionType type : QuestionType.values()) {
      String typeString = type.toString().toLowerCase();
      String link = controllers.admin.routes.AdminQuestionController.newOne(typeString).url();
      ATag linkTag =
          a().withHref(link)
              .withId(String.format("create-%s-question", typeString))
              .withClasses(
                  "block",
                  "p-3",
                  "bg-white",
                  "text-gray-600",
                  StyleUtils.hover("bg-gray-100", "text-gray-800"))
              .with(
                  Icons.questionTypeSvg(type, 24)
                      .withClasses(
                          "inline-block", "h-6", "w-6", "mr-1", "text-sm"))
              .with(
                  p(type.getLabel())
                      .withClasses(
                          "ml-2",
                          "mr-4",
                          "inline",
                          "text-sm",
                          "uppercase"));
      dropdown.with(linkTag);
    }
    return linkButton.with(dropdown);
  }

  private DivTag renderSummary(ActiveAndDraftQuestions activeAndDraftQuestions) {
    // The total question count should be equivalent to the number of rows in the displayed table,
    // where we have a single entry for a question that is active and has a draft.
    return div(String.format(
            "Total Questions: %d", activeAndDraftQuestions.getQuestionNames().size()))
        .withClasses("float-right", "text-base", "px-4", "my-2");
  }

  /** Renders the full table. */
  private TableTag renderQuestionTable(
      ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    return table()
        .withClasses("border", "border-gray-300", "shadow-md", "w-full")
        .with(renderQuestionTableHeaderRow())
        .with(
            tbody(
                each(
                    activeAndDraftQuestions.getQuestionNames(),
                    (questionName) ->
                        renderQuestionTableRow(
                            activeAndDraftQuestions.getActiveQuestionDefinition(questionName),
                            activeAndDraftQuestions.getDraftQuestionDefinition(questionName),
                            activeAndDraftQuestions.getDeletionStatus(questionName),
                            request))));
  }

  /** Render the question table header row. */
  private TheadTag renderQuestionTableHeaderRow() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4"))
            .with(th("Question text").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3"))
            .with(th("Supported languages").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/6"))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES,
                        "text-right",
                        "pr-8",
                        "w-1/6")));
  }

  /**
   * Display this as a table row with all fields.
   *
   * <p>One of {@code activeDefinition} and {@code draftDefinition} must be specified.
   */
  private TrTag renderQuestionTableRow(
      Optional<QuestionDefinition> activeDefinition,
      Optional<QuestionDefinition> draftDefinition,
      DeletionStatus deletionStatus,
      Http.Request request) {
    if (draftDefinition.isEmpty() && activeDefinition.isEmpty()) {
      throw new IllegalArgumentException("Did not receive a valid question.");
    }
    QuestionDefinition latestDefinition = draftDefinition.orElseGet(() -> activeDefinition.get());
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(latestDefinition))
        .with(renderQuestionTextCell(latestDefinition))
        .with(renderSupportedLanguages(latestDefinition))
        .with(renderActionsCell(activeDefinition, draftDefinition, deletionStatus, request));
  }

  private TdTag renderInfoCell(QuestionDefinition definition) {
    return td().with(div(definition.getName()).withClasses("font-semibold"))
        .with(div(definition.getDescription()).withClasses("text-xs"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
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

    return td().with(div(questionText).withClasses("font-semibold"))
        .with(div(questionHelpText).withClasses("text-xs"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
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
    return td().with(div(formattedLanguages))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private ATag renderQuestionEditLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.AdminQuestionController.edit(definition.getId()).url();
    return new LinkElement()
        .setId("edit-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles("mr-2")
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
        .setStyles("mr-2")
        .asAnchorText();
  }

  private ATag renderQuestionViewLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.AdminQuestionController.show(definition.getId()).url();
    return new LinkElement()
        .setId("view-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles("mr-2")
        .asAnchorText();
  }

  private TdTag renderActionsCell(
      Optional<QuestionDefinition> active,
      Optional<QuestionDefinition> draft,
      DeletionStatus deletionStatus,
      Http.Request request) {
    TdTag td = td().withClasses(BaseStyles.TABLE_CELL_STYLES, "text-right");
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
        .setStyles("mr-2")
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
        .setStyles("mr-2")
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
        .setStyles("mr-2")
        .asHiddenFormLink(request);
  }
}
