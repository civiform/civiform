package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.TranslationNotFoundException;
import views.BaseHtmlView;
import views.BaseStyles;
import views.StyleUtils;
import views.Styles;
import views.admin.AdminLayout;

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
    return renderLinkButton("Create new question", "/admin/questions/new").withId("createQuestion");
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
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5"))
            .with(th("Question text").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5"))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES, Styles.TEXT_RIGHT, Styles.PR_8, "w-1/5")));
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
      questionText = definition.getQuestionText(Locale.ENGLISH);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    try {
      questionHelpText = definition.getQuestionHelpText(Locale.ENGLISH);
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
            renderLink(
                linkText,
                link,
                String.join(
                    " ",
                    Styles.MR_2,
                    StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, Styles.MR_4))));
  }
}
