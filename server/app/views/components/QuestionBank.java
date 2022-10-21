package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.InputTag;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
public final class QuestionBank {
  private static final SvgTag PLUS_ICON =
      Icons.svg(Icons.PLUS)
          .withClasses("shrink-0", "h-12", "w-5")
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

    DivTag innerDiv = div().withClasses("shadow-lg", "h-full");
    questionForm.with(innerDiv);
    DivTag contentDiv = div().withClasses("relative", "grid", "gap-6", "px-5", "py-6");
    innerDiv.with(contentDiv);

    H1Tag headerDiv = h1("Add Question").withClasses("mx-2", "-mb-3", "text-xl");
    contentDiv.with(div().with(headerDiv));

    InputTag filterInput =
        input()
            .withId("question-bank-filter")
            .withType("text")
            .withName("questionFilter")
            .withPlaceholder("Search questions")
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

    SvgTag filterIcon = Icons.svg(Icons.SEARCH).withClasses("h-4", "w-4");
    DivTag filterIconDiv = div().withClasses("absolute", "ml-4", "mt-3", "mr-4").with(filterIcon);
    DivTag filterDiv = div().withClasses("mb-2", "relative").with(filterIconDiv, filterInput);
    contentDiv.with(filterDiv);
    contentDiv.with(
        div()
            .with(
                div()
                    .with(
                        p("Not finding a question you're looking for in this list?"),
                        div()
                            .withClass("flex")
                            .with(
                                div().withClass("flex-grow"),
                                CreateQuestionButton.renderCreateQuestionButton(
                                    params.questionCreateRedirectUrl(),
                                    /* isPrimaryButton= */ false)))));

    ImmutableList<QuestionDefinition> questions =
        filterQuestions()
            .sorted(
                Comparator.<QuestionDefinition, Instant>comparing(
                        qdef -> qdef.getLastModifiedTime().orElse(Instant.EPOCH))
                    .reversed()
                    .thenComparing(qdef -> qdef.getName().toLowerCase()))
            .collect(ImmutableList.toImmutableList());

    contentDiv.with(
        div()
            .withId("question-bank-questions")
            .with(each(questions, this::renderQuestionDefinition)));

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
                StyleUtils.hover("scale-105", "text-gray-800", "border", "border-gray-100"));

    ButtonTag addButton =
        TagCreator.button()
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(ReferenceClasses.ADD_QUESTION_BUTTON, AdminStyles.CLICK_TARGET_BUTTON);

    SvgTag icon =
        Icons.questionTypeSvg(definition.getQuestionType()).withClasses("shrink-0", "h-12", "w-6");
    String questionHelpText =
        definition.getQuestionHelpText().isEmpty()
            ? ""
            : definition.getQuestionHelpText().getDefault();
    DivTag content =
        div()
            .withClasses("ml-4")
            .with(
                p(definition.getQuestionText().getDefault())
                    .withClass(ReferenceClasses.ADMIN_QUESTION_TITLE),
                p(questionHelpText).withClasses("mt-1", "text-sm", "line-clamp-2"),
                p(String.format("Admin ID: %s", definition.getName()))
                    .withClasses("mt-1", "text-sm"),
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
  private Stream<QuestionDefinition> filterQuestions() {
    if (containsSingleBlockQuestion()) {
      return Stream.empty();
    }

    Predicate<QuestionDefinition> filter =
        params.blockDefinition().getQuestionCount() > 0
            ? this::nonEmptyBlockFilter
            : this::questionFilter;
    return params.questions().stream().filter(filter);
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
