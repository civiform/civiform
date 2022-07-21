package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.InputTag;
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


/** Contains methods for rendering question bank for an admin to add questions to a program. */
public class QuestionBank {
  private static final SvgTag PLUS_ICON =
      Icons.svg(Icons.PLUS, 24)
          .withClasses("flex-shrink-0", "h-12", "w-6")
          .attr("fill", "currentColor")
          .attr("stroke-width", "2")
          .attr("stroke-linecap", "round")
          .attr("stroke-linejoin", "round");

  private ProgramDefinition program;
  private BlockDefinition blockDefinition;
  private Optional<Long> enumeratorQuestionId;
  private ImmutableList<QuestionDefinition> questions = ImmutableList.of();
  private Optional<InputTag> maybeCsrfTag = Optional.empty();
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

  public QuestionBank setCsrfTag(InputTag csrfTag) {
    this.maybeCsrfTag = Optional.of(csrfTag);
    return this;
  }

  public QuestionBank setQuestions(ImmutableList<QuestionDefinition> questionDefinitions) {
    this.questions = questionDefinitions;
    return this;
  }

  public FormTag getContainer() {
    return questionBankPanel();
  }

  private FormTag questionBankPanel() {
    InputTag csrfTag = this.maybeCsrfTag.get();
    FormTag questionForm = form(csrfTag).withMethod(HttpVerbs.POST).withAction(questionAction);

    DivTag innerDiv = div().withClasses("shadow-lg", "overflow-hidden", "h-full");
    questionForm.with(innerDiv);
    DivTag contentDiv =
        div().withClasses("relative", "grid", "gap-6", "px-5", "py-6");
    innerDiv.with(contentDiv);

    H1Tag headerDiv = h1("Question bank").withClasses("mx-2", "-mb-3", "text-xl");
    contentDiv.withId("question-bank-questions").with(headerDiv);

    InputTag filterInput =
        input()
            .withId("question-bank-filter")
            .withType("text")
            .withName("questionFilter")
            .withPlaceholder("Filter questions")
            .withClasses(
                "h-10",
                "px-10",
                "pr-5",
                "w-full",
                "rounded-full",
                "text-sm",
                "border",
                "border-gray-200",
                "shadow",
                StyleUtils.focus("outline-none"));

    SvgTag filterIcon = Icons.svg(Icons.SEARCH, 56).withClasses("h-4", "w-4");
    DivTag filterIconDiv =
        div().withClasses("absolute", "ml-4", "mt-3", "mr-4").with(filterIcon);
    DivTag filterDiv = div().withClasses("relative").with(filterIconDiv).with(filterInput);
    contentDiv.with(filterDiv);

    ImmutableList<QuestionDefinition> filteredQuestions = filterQuestions();

    ImmutableList<QuestionDefinition> sortedQuestions =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(QuestionDefinition::getName), filteredQuestions);

    sortedQuestions.forEach(
        questionDefinition -> contentDiv.with(renderQuestionDefinition(questionDefinition)));

    return questionForm;
  }

  private DivTag renderQuestionDefinition(QuestionDefinition definition) {
    DivTag questionDiv =
        div()
            .withId("add-question-" + definition.getId())
            .withClasses(
                ReferenceClasses.QUESTION_BANK_ELEMENT,
                "relative",
                "-m-3",
                "p-3",
                "flex",
                "items-start",
                "rounded-lg",
                "border",
                "border-transparent",
                "transition-all",
                "transform",
                StyleUtils.hover(
                    "scale-105", "text-gray-800", "border", "border-gray-100"));

    ButtonTag addButton =
        TagCreator.button(text(definition.getName()))
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(ReferenceClasses.ADD_QUESTION_BUTTON, AdminStyles.CLICK_TARGET_BUTTON);

    SvgTag icon =
        Icons.questionTypeSvg(definition.getQuestionType(), 24)
            .withClasses("flex-shrink-0", "h-12", "w-6");
    DivTag content =
        div()
            .withClasses("ml-4")
            .with(
                p(definition.getName()),
                p(definition.getDescription()).withClasses("mt-1", "text-sm"),
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
