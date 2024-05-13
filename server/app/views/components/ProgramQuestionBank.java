package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static views.BaseHtmlView.iconOnlyButton;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.PTag;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.http.client.utils.URIBuilder;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import services.ProgramBlockValidation;
import services.ProgramBlockValidation.AddQuestionResult;
import services.ProgramBlockValidationFactory;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

/** Contains methods for rendering question bank for an admin to add questions to a program. */
public final class ProgramQuestionBank {

  // Url parameter used to force question bank open upon initial rendering
  // of program edit page.
  private static final String SHOW_QUESTION_BANK_PARAM = "sqb";
  // Data attribute used to store which text is relevant when filtering
  // questions via the search bar.
  private static final String RELEVANT_FILTER_TEXT_DATA_ATTR = "relevantfiltertext";

  private final ProgramQuestionBankParams params;
  private final ProgramBlockValidationFactory programBlockValidationFactory;

  /**
   * Possible states of question bank upon rendering. Normally it starts hidden and triggered by
   * user clicking "Add question" button. But in some cases, like returning to the program edit page
   * after adding a question - we want to render it visible.
   */
  public enum Visibility {
    VISIBLE,
    HIDDEN
  }

  public ProgramQuestionBank(
      ProgramQuestionBankParams params,
      ProgramBlockValidationFactory programBlockValidationFactory) {
    this.params = checkNotNull(params);
    this.programBlockValidationFactory = checkNotNull(programBlockValidationFactory);
  }

  public DivTag getContainer(Visibility questionBankVisibility) {
    return div()
        .withId(ReferenceClasses.QUESTION_BANK_CONTAINER)
        // For explanation of why we need two different hidden classes see
        // initToggleQuestionBankButtons() in questionBank.ts
        .withClasses(
            questionBankVisibility == Visibility.HIDDEN
                ? ReferenceClasses.QUESTION_BANK_HIDDEN
                : "",
            questionBankVisibility == Visibility.HIDDEN ? "hidden" : "",
            "fixed",
            "w-full",
            "h-screen")
        .with(
            div()
                .withClasses(
                    "bg-gray-400",
                    "opacity-75",
                    "h-full",
                    "w-full",
                    "cursor-pointer",
                    "transition-opacity",
                    ReferenceClasses.CLOSE_QUESTION_BANK_BUTTON,
                    ReferenceClasses.QUESTION_BANK_GLASSPANE))
        .with(questionBankPanel());
  }

  private FormTag questionBankPanel() {
    FormTag questionForm =
        form()
            .withMethod(HttpVerbs.POST)
            .withAction(params.questionAction())
            .with(params.csrfTag())
            .withClasses(
                ReferenceClasses.QUESTION_BANK_PANEL,
                "h-full",
                "bg-white",
                "w-1/2",
                "overflow-y-auto",
                "absolute",
                "right-0",
                "top-0",
                "transition-transform");

    // We set pb-12 (padding bottom 12) to account for the fact that question bank height is screen
    // size while it's effective space is screen-height minus header-height. That pushes question
    // bank
    // below the header and the bottom part is below the visible part of the screen. Because of that
    // we add pb-12 so that invisible part is empty and question are not partly cut.
    DivTag contentDiv = div().withClasses("relative", "grid", "gap-6", "px-5", "pt-6", "pb-12");
    questionForm.with(contentDiv);

    H1Tag headerDiv = h1("Add a question").withClasses("mx-2", "text-xl");
    contentDiv.with(
        div()
            .withClasses("flex", "items-center")
            .with(
                iconOnlyButton("Close")
                    .withClasses(
                        ReferenceClasses.CLOSE_QUESTION_BANK_BUTTON, ButtonStyles.CLEAR_WITH_ICON)
                    .with(
                        Icons.svg(Icons.CLOSE).withClasses("w-6", "h-6", "cursor-pointer", "mr-2")))
            .with(headerDiv));
    contentDiv.with(
        QuestionBank.renderFilterAndSort(
            ImmutableList.of(QuestionSortOption.LAST_MODIFIED, QuestionSortOption.ADMIN_NAME)));
    contentDiv.with(
        div()
            .with(
                div()
                    .withClasses("flex", "items-center", "justify-end")
                    .with(
                        p("Not finding a question you're looking for in this list?")
                            .withClass("mr-2"),
                        div()
                            .withClass("flex")
                            .with(
                                div().withClass("flex-grow"),
                                CreateQuestionButton.renderCreateQuestionButton(
                                    params.questionCreateRedirectUrl(),
                                    /* isPrimaryButton= */ false)))));

    // Sort by last modified, since that's the default of the sort by dropdown
    ImmutableList<QuestionDefinition> allQuestions =
        filterQuestions()
            .sorted(
                Comparator.<QuestionDefinition, Instant>comparing(
                        qdef -> qdef.getLastModifiedTime().orElse(Instant.EPOCH))
                    .reversed()
                    .thenComparing(qdef -> qdef.getName().toLowerCase(Locale.ROOT)))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<QuestionDefinition> universalQuestions =
        allQuestions.stream().filter(q -> q.isUniversal()).collect(ImmutableList.toImmutableList());
    ImmutableList<QuestionDefinition> nonUniversalQuestions =
        allQuestions.stream()
            .filter(q -> !q.isUniversal())
            .collect(ImmutableList.toImmutableList());
    if (!universalQuestions.isEmpty()) {
      contentDiv.with(
          div()
              .withId("question-bank-universal")
              .withClasses(ReferenceClasses.SORTABLE_QUESTIONS_CONTAINER)
              .with(h2("Universal questions").withClasses(AdminStyles.SEMIBOLD_HEADER))
              .with(
                  ViewUtils.makeAlertSlim(
                      "We recommend using all universal questions in your program for personal and"
                          + " contact information questions.",
                      /* hidden= */ false,
                      /* classes...= */ BaseStyles.ALERT_INFO))
              .with(each(universalQuestions, qd -> renderQuestionDefinition(qd))));
    }
    contentDiv.with(
        div()
            .withId("question-bank-nonuniversal")
            .withClass(ReferenceClasses.SORTABLE_QUESTIONS_CONTAINER)
            .condWith(
                !universalQuestions.isEmpty(),
                h2("All other questions").withClasses(AdminStyles.SEMIBOLD_HEADER))
            .with(each(nonUniversalQuestions, qd -> renderQuestionDefinition(qd))));

    return questionForm;
  }

  private DivTag renderQuestionDefinition(QuestionDefinition definition) {
    String questionHelpText =
        definition.getQuestionHelpText().isEmpty()
            ? ""
            : definition.getQuestionHelpText().getDefault();
    // Create a string containing all the text that should be indexed when
    // filtering questions.
    String relevantFilterText =
        String.join(
            " ",
            definition.getQuestionText().getDefault(),
            questionHelpText,
            definition.getName(),
            definition.getDescription());

    DivTag questionDiv =
        div()
            .withId("add-question-" + definition.getId())
            .withClasses(ReferenceClasses.QUESTION_BANK_ELEMENT, "border-b", "border-gray-300")
            .condWith(definition.isUniversal(), ViewUtils.makeUniversalBadge(definition, "mt-3"))
            .withData(QuestionSortOption.ADMIN_NAME.getDataAttribute(), definition.getName())
            .withData(
                QuestionSortOption.LAST_MODIFIED.getDataAttribute(),
                definition.getLastModifiedTime().orElse(Instant.EPOCH).toString())
            .withData(RELEVANT_FILTER_TEXT_DATA_ATTR, relevantFilterText);
    DivTag row = div().withClasses("relative", "p-3", "pr-0", "flex");

    ButtonTag addButton =
        button("Add")
            .withType("submit")
            .withId("question-" + definition.getId())
            .withName("question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(
                ReferenceClasses.ADD_QUESTION_BUTTON,
                ButtonStyles.OUTLINED_WHITE_WITH_ICON,
                "h-12");

    SvgTag icon =
        Icons.questionTypeSvg(definition.getQuestionType()).withClasses("shrink-0", "h-6", "w-6");

    // Only show the admin note if it is not empty.
    PTag adminNote =
        definition.getDescription().isEmpty()
            ? p()
            : p(String.format("Admin note: %s", definition.getDescription()))
                .withClasses("mt-1", "text-sm");
    DivTag content = div()
            .withClasses("ml-4", "grow")
            .with(
                    div()
                        .with(TextFormatter.formatText(definition.getQuestionText().getDefault()))
                        .withClasses(
                                ReferenceClasses.ADMIN_QUESTION_TITLE, "font-bold", "w-3/5", "break-all"),
                    div()
                        .with(TextFormatter.formatText(questionHelpText))
                        .withClasses("mt-1", "text-sm",
                            "line-clamp-2"),
                    p(String.format("Admin ID: %s", definition.getName()))
                            .withClasses("mt-1", "text-sm"),
                    adminNote);

    return questionDiv.with(row.with(icon, content, addButton));
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
    ProgramBlockValidation programBlockValidation = programBlockValidationFactory.create();
    return params.questions().stream()
        .filter(
            q ->
                programBlockValidation.canAddQuestion(params.program(), params.blockDefinition(), q)
                    == AddQuestionResult.ELIGIBLE);
  }

  /**
   * Question bank is hidden by default when user opens program edit page. But some actions, like
   * adding question from the bank, lead to page reload and after such reload question bank should
   * stay open as that matches user expectations.
   */
  public static String addShowQuestionBankParam(String url) {
    try {
      return new URIBuilder(url)
          .setParameter(ProgramQuestionBank.SHOW_QUESTION_BANK_PARAM, "true")
          .build()
          .toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Visibility shouldShowQuestionBank(Http.Request request) {
    return request
            .queryString(ProgramQuestionBank.SHOW_QUESTION_BANK_PARAM)
            .orElse("")
            .equals("true")
        ? Visibility.VISIBLE
        : Visibility.HIDDEN;
  }

  @AutoValue
  public abstract static class ProgramQuestionBankParams {
    abstract ProgramDefinition program();

    abstract BlockDefinition blockDefinition();

    abstract String questionCreateRedirectUrl();

    abstract ImmutableList<QuestionDefinition> questions();

    abstract InputTag csrfTag();

    abstract String questionAction();

    public static Builder builder() {
      return new AutoValue_ProgramQuestionBank_ProgramQuestionBankParams.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setProgram(ProgramDefinition v);

      public abstract Builder setBlockDefinition(BlockDefinition v);

      public abstract Builder setQuestionCreateRedirectUrl(String v);

      public abstract Builder setQuestions(ImmutableList<QuestionDefinition> v);

      public abstract Builder setCsrfTag(InputTag v);

      public abstract Builder setQuestionAction(String v);

      public abstract ProgramQuestionBankParams build();
    }
  }
}
