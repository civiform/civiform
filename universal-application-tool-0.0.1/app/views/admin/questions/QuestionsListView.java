package views.admin.questions;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.p;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import services.question.TranslationNotFoundException;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.components.Icons;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public final class QuestionsListView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionsListView(AdminLayout layout) {
    this.layout = layout;
  }

  /** Renders a page with a table view of all questions. */
  public Content render(ImmutableList<QuestionDefinition> questions, Optional<String> maybeFlash) {
    return layout.render(
        div(maybeFlash.orElse("")),
        renderHeader("All Questions"),
        div(renderQuestionTable(questions)).withClasses(Styles.M_4),
        renderAddQuestionLink(),
        renderSummary(questions));
  }

  private Tag renderAddQuestionLink() {
    String link = controllers.admin.routes.QuestionController.newOne("").url();
    String parentId = "create-question-button";
    ContainerTag linkButton = new LinkElement()
        .setId(parentId)
        .setHref(link)
        .setText("Create new question")
        .asButton();
    // for (QuestionType type : questionTypes)
    ContainerTag addressLink = a().withHref("#").withClasses("p-4 pr-12 hover:bg-gray-100")
      .with(Icons.questionTypeSvg(QuestionType.ADDRESS, 24).withClasses("inline flex-shrink-0 h-6 w-6 mr-1 text-sm"))
      .with(p("Address").withClasses("inline text-sm"));
    ContainerTag nameLink = a().withHref("#").withClasses("p-4 pr-12 hover:bg-gray-100")
      .with(Icons.questionTypeSvg(QuestionType.NAME, 24).withClasses("inline flex-shrink-0 h-6 w-6 mr-1 text-sm"))
      .with(p("Name").withClasses("inline text-sm"));

    ContainerTag dropdown = div(addressLink, nameLink).withId(parentId + "-dropdown").withClasses("border bg-white w-max text-gray-600 shadow-lg hidden absolute mt-3 transition-all scale-0");
    return linkButton.with(dropdown);
  }

  private Tag renderSummary(ImmutableList<QuestionDefinition> questions) {
    return div("Total Questions: " + questions.size())
        .withClasses(Styles.FLOAT_RIGHT, Styles.TEXT_BASE, Styles.PX_4, Styles.MY_2);
  }

  /** Renders the full table. */
  private Tag renderQuestionTable(ImmutableList<QuestionDefinition> questions) {
    return table()
        .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
        .with(renderQuestionTableHeaderRow())
        .with(tbody(each(questions, question -> renderQuestionTableRow(question))));
  }

  /** Render the question table header row. */
  private Tag renderQuestionTableHeaderRow() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5))
            .with(th("Question text").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES,
                        Styles.TEXT_RIGHT,
                        Styles.PR_8,
                        Styles.W_1_5)));
  }

  /** Display this as a table row with all fields. */
  private Tag renderQuestionTableRow(QuestionDefinition definition) {
    return tr().withClasses(
            Styles.BORDER_B, Styles.BORDER_GRAY_300, StyleUtils.even(Styles.BG_GRAY_100))
        .with(renderInfoCell(definition))
        .with(renderQuestionTextCell(definition))
        .with(renderActionsCell(definition));
  }

  private Tag renderInfoCell(QuestionDefinition definition) {
    return td().with(div(definition.getName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(definition.getDescription()).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private Tag renderQuestionTextCell(QuestionDefinition definition) {
    String questionText = "";
    String questionHelpText = "";

    try {
      questionText = definition.getQuestionText(Locale.US);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    try {
      questionHelpText = definition.getQuestionHelpText(Locale.US);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    return td().with(div(questionText).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(questionHelpText).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private Tag renderActionsCell(QuestionDefinition definition) {
    String linkText = "Edit →";
    String link = controllers.admin.routes.QuestionController.edit(definition.getId()).url();
    return td().withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.TEXT_RIGHT)
        .with(
            new LinkElement()
                .setId("edit-question-link-" + definition.getId())
                .setHref(link)
                .setText(linkText)
                .setStyles(Styles.MR_2)
                .asAnchorText());
  }
}
