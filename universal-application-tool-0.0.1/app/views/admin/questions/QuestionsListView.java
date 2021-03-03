package views.admin.questions;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.Tag;
import java.util.Optional;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class QuestionsListView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final ImmutableList<QuestionTableCell> tableCells =
      ImmutableList.of(
          QuestionTableCell.ID,
          QuestionTableCell.VERSION,
          QuestionTableCell.PATH,
          QuestionTableCell.NAME,
          QuestionTableCell.DESCRIPTION,
          QuestionTableCell.QUESTION_TEXT,
          QuestionTableCell.QUESTION_HELP_TEXT,
          QuestionTableCell.QUESTION_TYPE,
          QuestionTableCell.ACTIONS);

  @Inject
  public QuestionsListView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  /** Renders a page with either a table or a list view of all questions. */
  private Content render(ImmutableList<Tag> questionContent, Optional<String> maybeFlash) {
    return layout.htmlContent(
        body()
            .with(div(maybeFlash.orElse("")))
            .with(renderHeader("All Questions"))
            .with(questionContent)
            .with(renderAddQuestionLink()));
  }

  /** Renders a page with either a table view of all questions. */
  public Content renderAsTable(
      ImmutableList<QuestionDefinition> questions, Optional<String> maybeFlash) {
    return render(
        ImmutableList.of(renderQuestionTable(questions), renderSummary(questions)), maybeFlash);
  }

  /** Renders a page with either a list view of all questions. */
  public Content renderAsList(
      ImmutableList<QuestionDefinition> questions, Optional<String> maybeFlash) {
    ImmutableList.Builder<Tag> builder = ImmutableList.builder();
    for (QuestionDefinition qd : questions) {
      builder.add(renderQuestionDefinitionInfo(qd));
    }
    return render(builder.build(), maybeFlash);
  }

  private Tag renderAddQuestionLink() {
    return a("Create a new question").withHref("/admin/questions/new");
  }

  private Tag renderSummary(ImmutableList<QuestionDefinition> questions) {
    return div("Total Questions: " + questions.size());
  }

  /** Renders a div with some basic QuestionDefinition info in it. */
  private Tag renderQuestionDefinitionInfo(QuestionDefinition definition) {
    return div()
        .with(div(definition.getName()))
        .with(div(definition.getDescription()))
        .with(div(definition.getQuestionType().toString()));
  }

  /** Renders the full table. */
  private Tag renderQuestionTable(ImmutableList<QuestionDefinition> questions) {
    return table()
        .with(renderQuestionTableHeaderRow())
        .with(tbody(each(questions, question -> renderQuestionTableRow(question))));
  }

  /** Render the question table header row. */
  private Tag renderQuestionTableHeaderRow() {
    return thead(each(tableCells, tableCell -> th(tableCell.getHeaderText())));
  }

  /** Display this as a table row with all fields. */
  private Tag renderQuestionTableRow(QuestionDefinition definition) {
    return tr(each(tableCells, cell -> cell.getCellValue(definition)));
  }
}
