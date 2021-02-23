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
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class QuestionsListView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final ImmutableList<QuestionRow> tableCells =
      ImmutableList.of(
          QuestionRow.ID,
          QuestionRow.VERSION,
          QuestionRow.PATH,
          QuestionRow.NAME,
          QuestionRow.DESCRIPTION,
          QuestionRow.QUESTION_TEXT,
          QuestionRow.QUESTION_HELP_TEXT,
          QuestionRow.QUESTION_TYPE,
          QuestionRow.ACTIONS);

  @Inject
  public QuestionsListView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  /** Renders a page with either a table or a list view of all questions. */
  public Content render(ImmutableList<QuestionDefinition> questions, String renderAs) {
    return layout.htmlContent(
        body()
            .with(renderHeader("All Questions"))
            .with(renderAllQuestions(questions, renderAs))
            .with(renderSummary(questions))
            .with(renderAddQuestionLink()));
  }

  private Tag renderAddQuestionLink() {
    return a("Create a new question").withHref("/admin/questions/new");
  }

  /** Renders questions either as a table or a list. */
  private ImmutableList<Tag> renderAllQuestions(
      ImmutableList<QuestionDefinition> questions, String renderAs) {
    if (renderAs.equalsIgnoreCase("table")) {
      return ImmutableList.of(renderQuestionTable(questions));
    } else if (renderAs.equalsIgnoreCase("list")) {
      ImmutableList.Builder<Tag> builder = ImmutableList.builder();
      for (QuestionDefinition qd : questions) {
        builder.add(renderQuestionDefinitionInfo(qd, renderAs));
      }
      return builder.build();
    }
    return ImmutableList.of(div("Unknown render type: " + renderAs));
  }

  private Tag renderSummary(ImmutableList<QuestionDefinition> questions) {
    return div("Total Questions: " + questions.size());
  }

  /** Renders a div with some basic QuestionDefinition info in it. */
  private Tag renderQuestionDefinitionInfo(QuestionDefinition definition, String renderAs) {
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
