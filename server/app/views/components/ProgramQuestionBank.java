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
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.http.client.utils.URIBuilder;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.ProgramBlockValidation;
import services.ProgramBlockValidation.AddQuestionResult;
import services.ProgramBlockValidationFactory;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.ReferenceClasses;

/** Contains methods for rendering question bank for an admin to add questions to a program. */
public final class ProgramQuestionBank {

  // Url parameter used to force question bank open upon initial rendering
  // of program edit page.
  private static final String SHOW_QUESTION_BANK_PARAM = "sqb";
  // Url parameter used to send focus to the enumerator question section heading
  private static final String FOCUS_ENUMERATOR_HEADING_PARAM = "focusEnumeratorHeading";
  // Url parameter used to indicate a newly created question that should be auto-added to the block
  public static final String NEWLY_CREATED_QUESTION_ID_PARAM = "newQuestionId";
  // Data attribute used to store which text is relevant when filtering
  // questions via the search bar.
  private static final String RELEVANT_FILTER_TEXT_DATA_ATTR = "relevantfiltertext";

  private final ProgramQuestionBankParams params;
  private final ProgramBlockValidationFactory programBlockValidationFactory;
  private final SettingsManifest settingsManifest;
  private final Messages messages;
  private final Http.Request request;

  /**
   * Possible states of question bank upon rendering. Normally it starts hidden and triggered by
   * user clicking "Add question" button. But in some cases, like returning to the program edit page
   * after adding a question - we want to render it visible.
   */
  public enum Visibility {
    VISIBLE,
    HIDDEN
  }

  /**
   * The role the bank is playing for this render. The mode drives the form's submit endpoint
   * (regular {@code create} vs HTMX {@code hxSelectInitialQuestion}), whether non-enumerator
   * questions are filtered out, and whether the "Create new question" button is shown.
   */
  public enum Mode {
    /** Standard "Add question" flow — native POST to {@code create}, all eligible questions. */
    ANY_ELIGIBLE,
    /** "Choose existing" flow on an empty enumerator block — enumerator-only, no create button. */
    EXISTING_ENUMERATOR_ONLY,
    /** Initial-question selection — HTMX POST that swaps the chosen question into the form. */
    INITIAL_QUESTION
  }

  /** HTML id assigned to the bank's form element, used as the HTMX swap target for mode changes. */
  public static final String PANEL_FORM_ID = "question-bank-panel-form";

  public ProgramQuestionBank(
      ProgramQuestionBankParams params,
      ProgramBlockValidationFactory programBlockValidationFactory,
      SettingsManifest settingsManifest,
      Messages messages,
      Http.Request request) {
    this.params = checkNotNull(params);
    this.programBlockValidationFactory = checkNotNull(programBlockValidationFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.messages = checkNotNull(messages);
    this.request = checkNotNull(request);
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

  public FormTag questionBankPanel() {
    String headingId = "question-bank-heading";
    FormTag questionForm =
        form()
            .withId(PANEL_FORM_ID)
            .attr("aria-labelledby", headingId)
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
    applyModeAttrs(questionForm);

    // We set pb-12 (padding bottom 12) to account for the fact that question bank height is screen
    // size while it's effective space is screen-height minus header-height. That pushes question
    // bank
    // below the header and the bottom part is below the visible part of the screen. Because of that
    // we add pb-12 so that invisible part is empty and question are not partly cut.
    DivTag contentDiv = div().withClasses("relative", "grid", "gap-6", "px-5", "pt-6", "pb-12");
    questionForm.with(contentDiv);

    H1Tag headerDiv = h1("Add a question").withId(headingId).withClasses("mx-2", "text-xl");
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
    if (params.mode() != Mode.EXISTING_ENUMERATOR_ONLY) {
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
                                  CreateQuestionButton
                                      .renderCreateQuestionButtonForProgramQuestionBank(
                                          params.questionCreateRedirectUrl(),
                                          getParentEnumeratorId(),
                                          params.blockDefinition().isRepeated(),
                                          settingsManifest,
                                          request,
                                          /* isEmptyBlock= */ params
                                                  .blockDefinition()
                                                  .getQuestionCount()
                                              == 0,
                                          /* isInitialQuestion= */ params.mode()
                                              == Mode.INITIAL_QUESTION)))));
    }

    // Sort by last modified, since that's the default of the sort by dropdown
    ImmutableList<QuestionDefinition> allQuestions =
        filterQuestions()
            .sorted(
                Comparator.<QuestionDefinition, Instant>comparing(
                        qdef -> qdef.getLastModifiedTime().orElse(Instant.EPOCH))
                    .reversed()
                    .thenComparing(qdef -> qdef.getName().toLowerCase(Locale.ROOT)))
            .collect(ImmutableList.toImmutableList());

    boolean shouldShowPreviouslyUsedSection =
        settingsManifest.getEnumeratorImprovementsEnabled(request)
            && params.blockDefinition().isRepeated();

    ImmutableList<QuestionDefinition> previouslyUsedForRepeatedSetQuestions =
        shouldShowPreviouslyUsedSection
            ? allQuestions.stream()
                .filter(q -> q.getEnumeratorId().isPresent())
                .collect(ImmutableList.toImmutableList())
            : ImmutableList.of();

    ImmutableList<QuestionDefinition> remainingQuestions =
        allQuestions.stream()
            .filter(q -> !(shouldShowPreviouslyUsedSection && q.getEnumeratorId().isPresent()))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<QuestionDefinition> universalQuestions =
        remainingQuestions.stream()
            .filter(QuestionDefinition::isUniversal)
            .collect(ImmutableList.toImmutableList());
    ImmutableList<QuestionDefinition> nonUniversalQuestions =
        remainingQuestions.stream()
            .filter(q -> !q.isUniversal())
            .collect(ImmutableList.toImmutableList());

    if (!previouslyUsedForRepeatedSetQuestions.isEmpty()) {
      contentDiv.with(
          div()
              .withId("question-bank-previously-used")
              .withClasses(ReferenceClasses.SORTABLE_QUESTIONS_CONTAINER)
              .with(
                  h2(messages.at(MessageKey.HEADING_REPEATED_SET_PREVIOUSLY_USED.getKeyName()))
                      .withClasses(AdminStyles.SEMIBOLD_HEADER))
              .with(
                  AlertComponent.renderSlimInfoAlert(
                      messages.at(MessageKey.ALERT_REPEATED_SET_PREVIOUSLY_USED.getKeyName())))
              .with(
                  each(previouslyUsedForRepeatedSetQuestions, qd -> renderQuestionDefinition(qd))));
    }

    if (!universalQuestions.isEmpty()) {
      contentDiv.with(
          div()
              .withId("question-bank-universal")
              .withClasses(ReferenceClasses.SORTABLE_QUESTIONS_CONTAINER)
              .with(h2("Universal questions").withClasses(AdminStyles.SEMIBOLD_HEADER))
              .with(
                  AlertComponent.renderSlimInfoAlert(
                      "We recommend using all universal questions in your program for personal and"
                          + " contact information questions."))
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

    contentDiv.with(
        div()
            .with(
                Icons.questionTypeSvgWithId(QuestionType.ADDRESS),
                Icons.questionTypeSvgWithId(QuestionType.CURRENCY),
                Icons.questionTypeSvgWithId(QuestionType.CHECKBOX),
                Icons.questionTypeSvgWithId(QuestionType.DATE),
                Icons.questionTypeSvgWithId(QuestionType.DROPDOWN),
                Icons.questionTypeSvgWithId(QuestionType.EMAIL),
                Icons.questionTypeSvgWithId(QuestionType.ENUMERATOR),
                Icons.questionTypeSvgWithId(QuestionType.FILEUPLOAD),
                Icons.questionTypeSvgWithId(QuestionType.ID),
                Icons.questionTypeSvgWithId(QuestionType.NAME),
                Icons.questionTypeSvgWithId(QuestionType.NUMBER),
                Icons.questionTypeSvgWithId(QuestionType.RADIO_BUTTON),
                Icons.questionTypeSvgWithId(QuestionType.STATIC),
                Icons.questionTypeSvgWithId(QuestionType.TEXT),
                Icons.questionTypeSvgWithId(QuestionType.PHONE),
                Icons.questionTypeSvgWithId(QuestionType.NULL_QUESTION))
            .withClass("hidden"));

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
            .withClasses(ReferenceClasses.ADD_QUESTION_BUTTON, "question-bank-add-button");

    SvgTag icon =
        Icons.questionTypeSvgLink(definition.getQuestionType())
            .withClasses("shrink-0", "h-6", "w-6");

    // Only show the admin note if it is not empty.
    PTag adminNote =
        definition.getDescription().isEmpty()
            ? p()
            : p(String.format("Admin note: %s", definition.getDescription()))
                .withClasses("mt-1", "text-sm");

    DivTag content =
        div()
            .withClasses("ml-4", "grow")
            .with(
                div()
                    .with(
                        TextFormatter.formatTextForAdmins(
                            definition.getQuestionText().getDefault()))
                    .withClasses(
                        ReferenceClasses.ADMIN_QUESTION_TITLE, "font-bold", "w-3/5", "break-all"),
                div()
                    .with(TextFormatter.formatTextForAdmins(questionHelpText))
                    .withClasses("mt-1", "text-sm", "line-clamp-2"),
                p(String.format("Admin ID: %s", definition.getName()))
                    .withClasses("mt-1", "text-sm"),
                adminNote);

    return questionDiv.with(row.with(icon, content, addButton));
  }

  /**
   * Configures the form's submission attributes for the current {@link Mode}. INITIAL_QUESTION mode
   * uses HTMX to swap the selection into the enumerator-creation form without leaving the page; all
   * other modes use a native POST to the {@code create} endpoint.
   */
  private void applyModeAttrs(FormTag questionForm) {
    long programId = params.program().id();
    long blockId = params.blockDefinition().id();
    switch (params.mode()) {
      case INITIAL_QUESTION ->
          questionForm
              .attr(
                  "hx-post",
                  controllers.admin.routes.AdminProgramBlockQuestionsController
                      .hxSelectInitialQuestion(programId, blockId)
                      .url())
              .attr("hx-target", "#initial-question-slot")
              .attr("hx-swap", "outerHTML");
      case ANY_ELIGIBLE, EXISTING_ENUMERATOR_ONLY ->
          questionForm
              .withMethod(HttpVerbs.POST)
              .withAction(
                  controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                          programId, blockId)
                      .url());
    }
  }

  /**
   * If this is a repeated question, return the id of the enumerator question for the parent block.
   */
  private Optional<String> getParentEnumeratorId() {
    if (!settingsManifest.getEnumeratorImprovementsEnabled(request)
        || !params.blockDefinition().isRepeated()) {
      return Optional.empty();
    }

    try {
      BlockDefinition parentEnumeratorBlock =
          params.program().getBlockDefinition(params.blockDefinition().enumeratorId().get());
      if (!parentEnumeratorBlock.hasEnumeratorQuestion()) {
        return Optional.empty();
      }
      return Optional.of(
          Long.toString(parentEnumeratorBlock.getEnumeratorQuestionDefinition().getId()));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Used to filter questions in the question bank based on whether they are eligible to be added to
   * the current block.
   */
  private Stream<QuestionDefinition> filterQuestions() {
    ProgramBlockValidation programBlockValidation = programBlockValidationFactory.create();
    // EXISTING_ENUMERATOR_ONLY mode (the "Choose existing" flow on an empty enumerator block)
    // restricts the bank to enumerator-type questions; all other modes show everything eligible.
    boolean allowAllQuestions = params.mode() != Mode.EXISTING_ENUMERATOR_ONLY;
    return params.questions().stream()
        .filter(q -> allowAllQuestions || q.isEnumerator())
        .filter(
            q ->
                programBlockValidation.canAddQuestion(
                        params.program(),
                        params.blockDefinition(),
                        q,
                        settingsManifest.getEnumeratorImprovementsEnabled(request),
                        settingsManifest.getFileUploadQuestionImprovementsEnabled(request),
                        /* isInitialQuestion= */ params.mode() == Mode.INITIAL_QUESTION)
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

  /**
   * When an admin has chosen an existing question as the enumerator question, we want focus to be
   * sent to the section they were previously on after the page reloads.
   */
  public static String addFocusEnumeratorHeadingParam(String url) {
    try {
      return new URIBuilder(url)
          .setParameter(ProgramQuestionBank.FOCUS_ENUMERATOR_HEADING_PARAM, "true")
          .build()
          .toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds the newly created question ID to the URL so it can be auto-added to the block when the
   * user returns from question creation.
   */
  public static String addNewlyCreatedQuestionIdParam(String url, long questionId) {
    try {
      return new URIBuilder(url)
          .setParameter(
              ProgramQuestionBank.NEWLY_CREATED_QUESTION_ID_PARAM, String.valueOf(questionId))
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

    /** Drives the bank's submission target, filter, and CreateQuestionButton visibility. */
    abstract Mode mode();

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

      public abstract Builder setMode(Mode v);

      public abstract ProgramQuestionBankParams build();
    }
  }
}
