package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Comparator;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;
import views.StyleUtils;
import views.Styles;

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
    ContainerTag ret = div().withClasses(Styles.INLINE_BLOCK, Styles.W_1_4);
    ContainerTag innerDiv =
        div().withClasses(Styles.SHADOW_LG, Styles.OVERFLOW_HIDDEN, Styles.H_FULL);
    ret.with(innerDiv);
    ContainerTag contentDiv =
        div().withClasses(Styles.RELATIVE, Styles.GRID, Styles.GAP_6, Styles.PX_5, Styles.PY_6);
    innerDiv.with(contentDiv);

    ContainerTag headerDiv =
        h1("Question bank").withClasses(Styles.MX_2, Styles._MB_3, Styles.TEXT_XL);
    contentDiv.with(headerDiv);

    Tag filterInput =
        input()
            .withType("text")
            .withName("questionFilter")
            .attr(Attr.PLACEHOLDER, "Filter questions")
            .withClasses(
                Styles.H_10,
                Styles.PX_10,
                Styles.PR_5,
                Styles.W_FULL,
                Styles.ROUNDED_FULL,
                Styles.TEXT_SM,
                Styles.BORDER,
                Styles.BORDER_GRAY_200,
                Styles.SHADOW,
                StyleUtils.focus(Styles.OUTLINE_NONE));

    ContainerTag filterIcon =
        Icons.svg(Icons.SEARCH_SVG_PATH, 56).withClasses(Styles.H_4, Styles.W_4);
    ContainerTag filterIconDiv =
        div().withClasses(Styles.ABSOLUTE, Styles.ML_4, Styles.MT_3, Styles.MR_4).with(filterIcon);
    ContainerTag filterDiv = div(filterIconDiv, filterInput).withClasses(Styles.RELATIVE);

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
                Styles._M_3,
                Styles.P_3,
                Styles.FLEX,
                Styles.ITEMS_START,
                Styles.ROUNDED_LG,
                Styles.TRANSITION_ALL,
                Styles.TRANSFORM,
                StyleUtils.hover(
                    Styles.SCALE_105, Styles.TEXT_GRAY_800, Styles.BORDER, Styles.BORDER_GRAY_100));

    Tag addButton =
        TagCreator.button(text("+"))
            .attr(Attr.FORM, formId)
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(Styles.HIDDEN);

    ContainerTag icon =
        Icons.questionTypeSvg(definition.getQuestionType(), 24)
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    ContainerTag content =
        div()
            .withClasses(Styles.ML_4)
            .with(
                p(definition.getName()),
                p(definition.getDescription()).withClasses(Styles.MT_1, Styles.TEXT_SM),
                addButton);
    return ret.with(icon, content);
  }
}
