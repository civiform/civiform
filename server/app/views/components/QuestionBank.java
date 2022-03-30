package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
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
import java.util.Optional;
import java.util.function.Predicate;
import play.mvc.Http.HttpVerbs;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Contains methods for rendering question bank for an admin to add questions to a program. */
public class QuestionBank {
  private static final ContainerTag PLUS_ICON =
      Icons.svg(Icons.PLUS_SVG_PATH, 24)
          .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6)
          .attr("fill", "currentColor")
          .attr("stroke-width", "2")
          .attr("stroke-linecap", "round")
          .attr("stroke-linejoin", "round");

  private ProgramDefinition program;
  private BlockDefinition blockDefinition;
  private Optional<Long> enumeratorQuestionId;
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
    ContainerTag questionForm =
        form(this.csrfTag).withMethod(HttpVerbs.POST).withAction(questionAction);

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
    ContainerTag filterDiv =
        div().withClasses(Styles.RELATIVE).with(filterIconDiv).with(filterInput);
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
                ReferenceClasses.QUESTION_BANK_ELEMENT,
                Styles.RELATIVE,
                Styles._M_3,
                Styles.P_3,
                Styles.FLEX,
                Styles.ITEMS_START,
                Styles.ROUNDED_LG,
                Styles.BORDER,
                Styles.BORDER_TRANSPARENT,
                Styles.TRANSITION_ALL,
                StyleUtils.hover(Styles.SCALE_105, Styles.TEXT_GRAY_800, Styles.BORDER, Styles.BORDER_GRAY_100));

    Tag addButton =
        TagCreator.button(text(definition.getName()))
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(ReferenceClasses.ADD_QUESTION_BUTTON, AdminStyles.CLICK_TARGET_BUTTON);

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
    return questionDiv.with(PLUS_ICON, icon, content);
  }

  /**
   * Used to filter questions in the question bank.
   *
   * <p>Questions that are filtered out:
   *
   * <ul>
   *   <li>If there is at least one question in the current block, all single-block questions are
   *       filtered.
   *   <li>If there is a single block question in the current block, all questions are filtered.
   *   <li>If this is a repeated block, only the appropriate repeated questions are showed.
   *   <li>Questions already in the program are filtered.
   * </ul>
   */
  private ImmutableList<QuestionDefinition> filterQuestions() {
    if (containsSingleBlockQuestion()) {
      return ImmutableList.of();
    }

    Predicate<QuestionDefinition> filter =
        blockDefinition.getQuestionCount() > 0 ? this::nonEmptyBlockFilter : this::questionFilter;
    return questions.stream().filter(filter).collect(ImmutableList.toImmutableList());
  }

  /** If a block already contains a single-block question, no more questions can be added. */
  private boolean containsSingleBlockQuestion() {
    return blockDefinition.isEnumerator() || blockDefinition.isFileUpload();
  }

  /**
   * An block can add questions with the appropriate enumerator id that aren't already in this
   * program.
   */
  private boolean questionFilter(QuestionDefinition questionDefinition) {
    return questionDefinition.getEnumeratorId().equals(getEnumeratorQuestionId())
        && !program.hasQuestion(questionDefinition);
  }

  /**
   * A non-empty block cannot add single-block questions, in addition to {@link
   * QuestionBank#questionFilter}.
   */
  private boolean nonEmptyBlockFilter(QuestionDefinition questionDefinition) {
    return !singleBlockQuestion(questionDefinition) && questionFilter(questionDefinition);
  }

  private boolean singleBlockQuestion(QuestionDefinition questionDefinition) {
    switch (questionDefinition.getQuestionType()) {
      case ENUMERATOR:
      case FILEUPLOAD:
        return true;
      default:
        return false;
    }
  }

  /**
   * Follow the {@link BlockDefinition#enumeratorId()} reference to the enumerator block definition,
   * and return the id of its {@link EnumeratorQuestionDefinition}.
   */
  private Optional<Long> getEnumeratorQuestionId() {
    if (enumeratorQuestionId == null) {
      enumeratorQuestionId = Optional.empty();
      Optional<Long> enumeratorBlockId = blockDefinition.enumeratorId();
      if (enumeratorBlockId.isPresent()) {
        try {
          BlockDefinition enumeratorBlockDefinition =
              program.getBlockDefinition(enumeratorBlockId.get());
          enumeratorQuestionId =
              Optional.of(enumeratorBlockDefinition.getQuestionDefinition(0).getId());
        } catch (ProgramBlockDefinitionNotFoundException e) {
          String errorMessage =
              String.format(
                  "BlockDefinition %d has a broken enumerator block reference to id %d",
                  blockDefinition.id(), enumeratorBlockId.get());
          throw new RuntimeException(errorMessage, e);
        }
        ;
      }
    }
    return enumeratorQuestionId;
  }
}
