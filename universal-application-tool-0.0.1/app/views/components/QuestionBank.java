package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Comparator;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;

public class QuestionBank {
  private ProgramDefinition program;
  private ImmutableList<QuestionDefinition> questions = ImmutableList.of();
  private String formId = "";

  public QuestionBank setProgram(ProgramDefinition program) {
    this.program = program;
    return this;
  }

  public QuestionBank setFormId(String formId) {
    this.formId = formId;
    return this;
  }

  public QuestionBank setQuestions(ImmutableList<QuestionDefinition> questionDefinitions) {
    this.questions = questionDefinitions;
    return this;
  }

  public ContainerTag getContainer() {
    return questionBankPanel();
  }

  private ContainerTag questionBankPanel() {
    ContainerTag ret = div().withClasses("inline-block w-1/4");
    ContainerTag innerDiv = div().withClasses("shadow-lg overflow-hidden h-full");
    ret.with(innerDiv);
    ContainerTag contentDiv = div().withClasses("relative grid gap-6 bg-secondary px-5 py-6");
    innerDiv.with(contentDiv);

    ContainerTag headerDiv = h1("Question bank").withClasses("mx-2 -mb-3 text-xl");
    contentDiv.with(headerDiv);

    Tag filterInput =
        input()
            .withType("text")
            .withName("questionFilter")
            .attr("placeholder", "Filter questions")
            .withClasses(
                "h-10 px-10 pr-5 w-full rounded-full text-sm border border-gray-200"
                    + " focus:outline-none shadow bg-grey-500 text-secondaryText");

    ContainerTag filterIcon = Icons.svg(Icons.SEARCH_SVG_PATH, 56).withClasses("h-4 w-4");
    ContainerTag filterIconDiv = div().withClasses("absolute ml-4 mt-3 mr-4").with(filterIcon);
    ContainerTag filterDiv =
        div(filterIconDiv, filterInput).withClasses("relative text-primaryText w-85");

    contentDiv.with(filterDiv);

    ImmutableList<QuestionDefinition> sortedQuestions =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(QuestionDefinition::getName), this.questions);

    sortedQuestions.stream()
        .filter(question -> !program.hasQuestion(question))
        .forEach(
            questionDefinition -> contentDiv.with(renderQuestionDefinition(questionDefinition)));

    return ret;
  }

  private ContainerTag renderQuestionDefinition(QuestionDefinition definition) {
    ContainerTag ret =
        div()
            .attr(
                "onclick",
                String.format("document.getElementById('question-%d').click()", definition.getId()))
            .withClasses(
                "-m-3 p-3 flex items-start rounded-lg hover:text-gray-800"
                    + " transition-all transform hover:scale-105");

    Tag addButton =
        TagCreator.button(text("+"))
            .attr("form", formId)
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses("hidden");

    ContainerTag icon =
        Icons.questionTypeSvg(definition.getQuestionType(), 24)
            .withClasses("flex-shrink-0 h-12 w-6 text-primaryText");
    ContainerTag content =
        div()
            .withClasses("ml-4")
            .with(
                p(definition.getName()).withClasses("text-base font-medium text-primaryText"),
                p(definition.getDescription()).withClasses("mt-1 text-sm text-secondaryText"),
                addButton);
    return ret.with(icon, content);
  }
}
