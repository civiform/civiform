package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;
import static views.ViewUtils.POST;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import services.program.BlockDefinition;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class QuestionBank {
  private ProgramDefinition program;
  private BlockDefinition blockDefinition;
  private Optional<Long> repeaterQuestionId;
  private ImmutableList<QuestionDefinition> questions = ImmutableList.of();
  private Tag csrfTag = div();
  private String questionAction = "";

  public QuestionBank setProgram(ProgramDefinition program) {
    this.program = program;
    return this;
  }

  public QuestionBank setBlockDefinition(BlockDefinition blockDefinition) {
    this.blockDefinition = blockDefinition;
    return this;
  }

  public QuestionBank setQuestionAction(String actionUrl) {
    this.questionAction = actionUrl;
    return this;
  }

  public QuestionBank setCsrfTag(Tag csrfTag) {
    this.csrfTag = csrfTag;
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
    ContainerTag questionForm = form(this.csrfTag).withMethod(POST).withAction(questionAction);

    div().withClasses(Styles.INLINE_BLOCK, Styles.W_1_4);
    ContainerTag innerDiv =
        div().withClasses(Styles.SHADOW_LG, Styles.OVERFLOW_HIDDEN, Styles.H_FULL);
    questionForm.with(innerDiv);
    ContainerTag contentDiv =
        div().withClasses(Styles.RELATIVE, Styles.GRID, Styles.GAP_6, Styles.PX_5, Styles.PY_6);
    innerDiv.with(contentDiv);

    ContainerTag headerDiv =
        h1("Question bank").withClasses(Styles.MX_2, Styles._MB_3, Styles.TEXT_XL);
    contentDiv.withId("question-bank-questions").with(headerDiv);

    Tag filterInput =
        input()
            .withId("question-bank-filter")
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

    ImmutableList<QuestionDefinition> filteredQuestions = filterQuestions();

    ImmutableList<QuestionDefinition> sortedQuestions =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(QuestionDefinition::getName), filteredQuestions);

    sortedQuestions.forEach(
        questionDefinition -> contentDiv.with(renderQuestionDefinition(questionDefinition)));

    return questionForm;
  }

  private ContainerTag renderQuestionDefinition(QuestionDefinition definition) {
    ContainerTag questionDiv =
        div()
            .withId("add-question-" + definition.getId())
            .withClasses(
                Styles.RELATIVE,
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
        TagCreator.button(text(definition.getName()))
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(ReferenceClasses.ADD_QUESTION_BUTTON, BaseStyles.CLICK_TARGET_BUTTON);

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
    return questionDiv.with(icon, content);
  }

  /**
   * Used to filter questions in the question bank.
   *
   * <p>Questions that are filtered out:
   *
   * <ul>
   *   <li>If there is at least one question in the current block, all repeater questions
   *   <li>If this is a repeated block only show the appropriate repeated questions
   *   <li>Questions already in the program
   * </ul>
   */
  private ImmutableList<QuestionDefinition> filterQuestions() {
    // A repeater block cannot add any more questions
    if (blockDefinition.isRepeater()) {
      return ImmutableList.of();
    }

    Predicate<QuestionDefinition> filter =
        blockDefinition.getQuestionCount() > 0 ? this::nonEmptyBlockFilter : this::questionFilter;
    return questions.stream().filter(filter).collect(ImmutableList.toImmutableList());
  }

  /**
   * An block can add questions with the appropriate repeater id that aren't already in this
   * program.
   */
  private boolean questionFilter(QuestionDefinition questionDefinition) {
    return questionDefinition.getRepeaterId().equals(getRepeaterQuestionId())
        && !program.hasQuestion(questionDefinition);
  }

  /**
   * A non-empty block cannot add repeater questions, in addition to {@link
   * QuestionBank#questionFilter}.
   */
  private boolean nonEmptyBlockFilter(QuestionDefinition questionDefinition) {
    return !questionDefinition.getQuestionType().equals(QuestionType.REPEATER)
        && questionFilter(questionDefinition);
  }

  /**
   * Follow the {@link BlockDefinition#repeaterId()} reference to the repeater block definition, and
   * return the id of its {@link services.question.types.RepeaterQuestionDefinition}.
   */
  private Optional<Long> getRepeaterQuestionId() {
    if (repeaterQuestionId == null) {
      repeaterQuestionId = Optional.empty();
      Optional<Long> repeaterBlockId = blockDefinition.repeaterId();
      if (repeaterBlockId.isPresent()) {
        try {
          BlockDefinition repeaterBlockDefinition =
              program.getBlockDefinition(repeaterBlockId.get());
          repeaterQuestionId =
              Optional.of(repeaterBlockDefinition.getQuestionDefinition(0).getId());
        } catch (ProgramBlockNotFoundException e) {
          String errorMessage =
              String.format(
                  "BlockDefinition %d has a broken repeater block reference to id %d",
                  blockDefinition.id(), repeaterBlockId.get());
          throw new RuntimeException(errorMessage, e);
        }
        ;
      }
    }
    return repeaterQuestionId;
  }
}
