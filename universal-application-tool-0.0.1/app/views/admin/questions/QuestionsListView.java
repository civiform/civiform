package views.admin.questions;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.span;
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
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class QuestionsListView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public QuestionsListView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render(ImmutableList<QuestionDefinition> questions, String renderAs) {
    return layout.htmlContent(
        body()
            .with(renderHeader("All Questions"))
            .with(renderAllQuestions(questions, renderAs))
            .with(renderSummary(questions))
            .with(renderAddQuestionLink()));
  }

  public Tag renderAddQuestionLink() {
    return a("Create a new question").withHref("/admin/questions/new");
  }

  public ImmutableList<Tag> renderAllQuestions(
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

  public Tag renderSummary(ImmutableList<QuestionDefinition> questions) {
    return div("Total Questions: " + questions.size());
  }

  public Tag renderQuestionDefinitionInfo(QuestionDefinition definition, String renderAs) {
    return div()
        .with(div(definition.getName()))
        .with(div(definition.getDescription()))
        .with(div(definition.getQuestionType().toString()));
  }

  public Tag renderQuestionTable(ImmutableList<QuestionDefinition> questions) {
    return table()
        .with(renderQuestionTableHeaderRow())
        .with(tbody(each(questions, question -> renderQuestionTableRow(question))));
  }

  public Tag renderQuestionTableHeaderRow() {
    ImmutableList<String> headerCells =
        ImmutableList.of(
            "Path",
            "Id",
            "Version",
            "Name",
            "Description",
            "Question Text",
            "Question Help Text",
            "Question Type",
            "Actions");
    return thead(each(headerCells, headerCell -> th(headerCell)));
  }

  /** Display this as a table row with all fields. */
  public Tag renderQuestionTableRow(QuestionDefinition definition) {
    String text = "";
    String helpText = "";
    try {
      text = definition.getQuestionText(Locale.ENGLISH);
      helpText = definition.getQuestionHelpText(Locale.ENGLISH);
    } catch (Exception e) {
    }
    return tr(
        td(definition.getPath()),
        td("" + definition.getId()),
        td("" + definition.getVersion()),
        td(definition.getName()),
        td(definition.getDescription()),
        td(text),
        td(helpText),
        td(definition.getQuestionType().toString()),
        td().with(span("view"))
            .with(span(" | "))
            .with(a("edit").withHref("/admin/questions/edit/" + definition.getPath())));
  }
}
