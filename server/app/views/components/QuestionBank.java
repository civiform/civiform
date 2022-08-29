package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.auto.value.AutoValue;
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
import views.style.Styles;

/** Contains methods for rendering question bank for an admin to add questions to a program. */
public final class QuestionBank {
  private static final SvgTag PLUS_ICON =
      Icons.svg(Icons.PLUS)
          .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_5)
          .attr("fill", "currentColor")
          .attr("stroke-width", "2")
          .attr("stroke-linecap", "round")
          .attr("stroke-linejoin", "round");

  private final QuestionBankParams params;
  private Optional<Long> enumeratorQuestionId;

  public QuestionBank(QuestionBankParams params) {
    this.params = checkNotNull(params);
  }

  public FormTag getContainer() {
    return questionBankPanel();
  }

  private FormTag questionBankPanel() {
    FormTag questionForm =
        form()
            .withMethod(HttpVerbs.POST)
            .withAction(params.questionAction())
            .with(params.csrfTag());

    DivTag innerDiv = div().withClasses(Styles.SHADOW_LG, Styles.OVERFLOW_HIDDEN, Styles.H_FULL);
    questionForm.with(innerDiv);
    DivTag contentDiv =
        div().withClasses(Styles.RELATIVE, Styles.GRID, Styles.GAP_6, Styles.PX_5, Styles.PY_6);
    innerDiv.with(contentDiv);

    H1Tag headerDiv = h1("Add Question").withClasses(Styles.MX_2, Styles._MB_3, Styles.TEXT_XL);
    contentDiv.with(div().with(headerDiv));

    InputTag filterInput =
        input()
            .withId("question-bank-filter")
            .withType("text")
            .withName("questionFilter")
            .withPlaceholder("Search questions")
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

    SvgTag filterIcon = Icons.svg(Icons.SEARCH).withClasses(Styles.H_4, Styles.W_4);
    DivTag filterIconDiv =
        div().withClasses(Styles.ABSOLUTE, Styles.ML_4, Styles.MT_3, Styles.MR_4).with(filterIcon);
    DivTag filterDiv =
        div().withClasses(Styles.MB_2, Styles.RELATIVE).with(filterIconDiv, filterInput);
    contentDiv.with(filterDiv);
    contentDiv.with(
        div()
            .with(
                div()
                    .with(
                        p("Not finding a question you're looking for in this list?"),
                        div()
                            .withClass(Styles.FLEX)
                            .with(
                                div().withClass(Styles.FLEX_GROW),
                                CreateQuestionButton.renderCreateQuestionButton(
                                    params.questionCreateRedirectUrl())))));

    ImmutableList<QuestionDefinition> filteredQuestions = filterQuestions();

    ImmutableList<QuestionDefinition> sortedQuestions =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(QuestionDefinition::getName), filteredQuestions);

    DivTag questionsDiv = div().withId("question-bank-questions");
    sortedQuestions.forEach(
        questionDefinition -> questionsDiv.with(renderQuestionDefinition(questionDefinition)));
    contentDiv.with(questionsDiv);

    return questionForm;
  }

  private DivTag renderQuestionDefinition(QuestionDefinition definition) {
    DivTag questionDiv =
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
                Styles.TRANSFORM,
                StyleUtils.hover(
                    Styles.SCALE_105, Styles.TEXT_GRAY_800, Styles.BORDER, Styles.BORDER_GRAY_100));

    ButtonTag addButton =
        TagCreator.button(text(definition.getName()))
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(ReferenceClasses.ADD_QUESTION_BUTTON, AdminStyles.CLICK_TARGET_BUTTON);

    SvgTag icon =
        Icons.questionTypeSvg(definition.getQuestionType())
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    DivTag content =
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
        params.blockDefinition().getQuestionCount() > 0
            ? this::nonEmptyBlockFilter
            : this::questionFilter;
    return params.questions().stream().filter(filter).collect(ImmutableList.toImmutableList());
  }

  /** If a block already contains a single-block question, no more questions can be added. */
  private boolean containsSingleBlockQuestion() {
    return params.blockDefinition().isEnumerator() || params.blockDefinition().isFileUpload();
  }

  /**
   * An block can add questions with the appropriate enumerator id that aren't already in this
   * program.
   */
  private boolean questionFilter(QuestionDefinition questionDefinition) {
    return questionDefinition.getEnumeratorId().equals(getEnumeratorQuestionId())
        && !params.program().hasQuestion(questionDefinition);
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
      Optional<Long> enumeratorBlockId = params.blockDefinition().enumeratorId();
      if (enumeratorBlockId.isPresent()) {
        try {
          BlockDefinition enumeratorBlockDefinition =
              params.program().getBlockDefinition(enumeratorBlockId.get());
          enumeratorQuestionId =
              Optional.of(enumeratorBlockDefinition.getQuestionDefinition(0).getId());
        } catch (ProgramBlockDefinitionNotFoundException e) {
          String errorMessage =
              String.format(
                  "BlockDefinition %d has a broken enumerator block reference to id %d",
                  params.blockDefinition().id(), enumeratorBlockId.get());
          throw new RuntimeException(errorMessage, e);
        }
        ;
      }
    }
    return enumeratorQuestionId;
  }

  @AutoValue
  public abstract static class QuestionBankParams {
    abstract ProgramDefinition program();

    abstract BlockDefinition blockDefinition();

    abstract String questionCreateRedirectUrl();

    abstract ImmutableList<QuestionDefinition> questions();

    abstract InputTag csrfTag();

    abstract String questionAction();

    public static Builder builder() {
      return new AutoValue_QuestionBank_QuestionBankParams.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setProgram(ProgramDefinition v);

      public abstract Builder setBlockDefinition(BlockDefinition v);

      public abstract Builder setQuestionCreateRedirectUrl(String v);

      public abstract Builder setQuestions(ImmutableList<QuestionDefinition> v);

      public abstract Builder setCsrfTag(InputTag v);

      public abstract Builder setQuestionAction(String v);

      public abstract QuestionBankParams build();
    }
  }
}
