package views.admin.migration;

import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import java.util.Objects;
import java.util.Optional;
import play.mvc.Http;
import services.AlertType;
import services.RandomStringUtils;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.AlertComponent;
import views.BaseHtmlView;
import views.admin.QuestionCard;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;

/** An HTMX partial for portions of the page rendered by {@link AdminImportView}. */
public final class AdminImportViewPartial extends BaseHtmlView {
  /**
   * The ID for the div containing the imported program data. Must be applied to the top-level DOM
   * element of each partial so that replacement works correctly.
   */
  public static final String PROGRAM_DATA_ID = "program-data";

  /** Renders an error that occurred while trying to parse the program data. */
  public DomContent renderError(String title, String errorMessage) {
    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            AlertComponent.renderFullAlert(
                AlertType.ERROR,
                /* text= */ errorMessage,
                /* title= */ Optional.of(title),
                /* hidden= */ false),
            asRedirectElement(button("Try again"), routes.AdminImportController.index().url())
                .withClasses("my-5", "usa-button", "usa-button--outline"));
  }

  /** Renders an error with properly formatted line breaks for multiple validation errors. */
  public DomContent renderErrorWithLineBreaks(String title, String errorMessage) {
    Iterable<String> errorLines = Splitter.on(". ").split(errorMessage);

    DivTag errorDiv =
        div()
            .withClasses("usa-alert", "usa-alert--error", "usa-alert--no-icon")
            .attr("role", "alert");

    DivTag bodyDiv = div().withClass("usa-alert__body");
    if (title != null) {
      bodyDiv.with(h3(title).withClass("usa-alert__heading"));
    }

    // Add each error line as a separate paragraph
    for (String line : errorLines) {
      if (!line.trim().isEmpty()) {
        bodyDiv.with(p("• " + line.trim()).withClass("usa-alert__text"));
      }
    }

    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            errorDiv.with(bodyDiv),
            asRedirectElement(button("Try again"), routes.AdminImportController.index().url())
                .withClasses("my-5", "usa-button", "usa-button--outline"));
  }

  /**
   * Renders the correctly parsed program data.
   *
   * @param request the HTTP request
   * @param program the program definition to be imported
   * @param questions the list of question definitions associated with the program
   * @param duplicateQuestionNames list of question names that are duplicates of existing questions
   * @param json the JSON representation of the program being imported
   * @return the rendered HTML content for the program data
   */
  public DomContent renderProgramData(
      Http.Request request,
      ProgramDefinition program,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> duplicateQuestionNames,
      String json) {

    DivTag questionAlert = buildQuestionAlert(questions, duplicateQuestionNames);

    DivTag programDiv = renderProgramDiv(program, questionAlert, questions);
    ImmutableMap<Long, QuestionDefinition> questionsById = ImmutableMap.of();

    // If there are no questions in the program, the "questions" field will not be included in the
    // JSON and questions will be null here.
    // We use the regular questions definition to construct this map.
    if (questions != null) {
      questionsById =
          questions.stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    DivTag formButtons =
        div()
            .with(
                submitButton("Save").withClasses("usa-button", "mr-2").withId("hx-submit-button"),
                asRedirectElement(
                        button("Delete and start over"), routes.AdminImportController.index().url())
                    .withClasses("usa-button", "usa-button--outline"))
            .withClasses("flex", "my-5");

    // In the duplicate-handling UI, all program content is displayed inside this form, so that the
    // radio options may be submitted along with the program JSON.
    FormTag programForm =
        form()
            .attr("hx-encoding", "application/x-www-form-urlencoded")
            .attr("hx-post", routes.AdminImportController.hxSaveProgram().url())
            .attr("hx-target", "#" + AdminImportViewPartial.PROGRAM_DATA_ID)
            .attr("hx-swap", "outerHTML")
            .attr("hx-indicator", "#hx-submit-button")
            .attr("hx-disabled-elt", "#hx-submit-button")
            .with(makeCsrfTokenInputTag(request))
            .with(
                FieldWithLabel.textArea()
                    .setFieldName(AdminProgramImportForm.PROGRAM_JSON_FIELD)
                    .setValue(json)
                    .getTextareaTag()
                    .withClass("hidden"))
            .condWith(
                duplicateQuestionNames.size() > 1, renderToplevelDuplicateQuestionHandlingOptions())
            .withAction(routes.AdminImportController.hxSaveProgram().url());

    for (BlockDefinition block : program.blockDefinitions()) {
      // Before the duplicate-handling UI is launched, we still keep the form separate (solely for
      // JSON), and display everything else in a regular div ahead of it.
      programForm.with(renderProgramBlock(block, questionsById, duplicateQuestionNames));
    }

    return programDiv.with(programForm.with(formButtons));
  }

  /** Renders a top-level program container. */
  private DivTag renderProgramDiv(
      ProgramDefinition program,
      DivTag questionAlert,
      ImmutableList<QuestionDefinition> questions) {
    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            h3("Program preview"),
            AlertComponent.renderSlimInfoAlert(
                /* text= */ "Please review the program name and details before saving.",
                /* classes...= */ "mb-2"))
        .condWith(!questions.isEmpty(), questionAlert)
        .with(
            h2(program.localizedName().getDefault()).withClasses("mb-2", "font-semibold"),
            h4("Admin name: " + program.adminName()).withClass("mb-2"));
  }

  /** Renders a message saying the program was successfully saved. */
  public DomContent renderProgramSaved(String programName, Long programId) {
    String successText =
        programName
            + " and its questions have been imported to your program dashboard. To view it,  visit"
            + " the program dashboard.";
    return div()
        .with(
            AlertComponent.renderFullAlert(
                AlertType.SUCCESS,
                /* text= */ successText,
                /* title= */ Optional.of("Your program has been successfully imported"),
                /* hidden= */ false,
                /* classes...= */ "mb-2"),
            div()
                .with(
                    asRedirectElement(
                            button("View program"),
                            routes.AdminProgramBlocksController.index(programId).url())
                        .withClasses("usa-button", "mr-2"),
                    asRedirectElement(
                            button("Import another program"),
                            routes.AdminImportController.index().url())
                        .withClasses("usa-button", "usa-button--outline"))
                .withClasses("flex", "my-5"));
  }

  private DivTag buildQuestionAlert(
      ImmutableList<QuestionDefinition> questions, ImmutableList<String> duplicateQuestionNames) {
    int numDuplicateQuestions = duplicateQuestionNames.size();
    int numNewQuestions = questions.size() - numDuplicateQuestions;

    AlertType alertType = AlertType.INFO;
    String alertMessage = "";

    alertMessage += "This program ";
    if (numNewQuestions > 0) {
      alertMessage +=
          String.format(
              "will add %d new question%s to the question bank",
              numNewQuestions, numNewQuestions > 1 ? "s" : "");
      if (numDuplicateQuestions > 0) {
        alertMessage += " and ";
      }
    }
    if (numDuplicateQuestions > 0) {
      alertType = AlertType.WARNING;
      alertMessage +=
          String.format(
              "contains %d duplicate question%s that must be resolved",
              numDuplicateQuestions, numDuplicateQuestions > 1 ? "s" : "");
    }
    alertMessage += ".";
    return AlertComponent.renderSlimAlert(
        alertType, alertMessage, /* hidden= */ false, /* classes...= */ "mb-2");
  }

  private DomContent renderProgramBlock(
      BlockDefinition block,
      ImmutableMap<Long, QuestionDefinition> questionsById,
      ImmutableList<String> duplicateQuestionNames) {
    DivTag blockDiv =
        div()
            .withClasses("border-t", "border-gray-200", "pt-2")
            .with(h4(block.name()).withClasses("font-semibold"), p(block.description()));
    // TODO: #7087 - Display eligibility and visibility predicates.

    if (!questionsById.isEmpty()) {
      for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
        blockDiv.with(
            renderQuestionCard(
                Objects.requireNonNull(questionsById.get(question.id())),
                duplicateQuestionNames,
                questionsById));
      }
    }

    return blockDiv;
  }

  /**
   * Renders an individual question, including the admin ID, help text, and any options or tags that
   * should be shown next to the question in the list of questions.
   */
  private DivTag renderQuestionCard(
      QuestionDefinition questionDefinition,
      ImmutableList<String> duplicateQuestionNames,
      ImmutableMap<Long, QuestionDefinition> questionsById) {
    // We use the old admin name (the one inputted by the admin) rather than the de-duped suffixed
    // name, since the admin has not yet decided how to handle this duplicate question. In the event
    // they choose to create a new duplicate, then the de-duped suffixed name will be used by the
    // backend to create a new question.
    boolean questionIsDuplicate = duplicateQuestionNames.contains(questionDefinition.getName());
    boolean questionIsRepeated = questionDefinition.getEnumeratorId().isPresent();

    return QuestionCard.renderForImport(
            questionDefinition,
            questionIsDuplicate ? makeDuplicateQuestionBadge() : makeNewQuestionBadge(),
            questionIsDuplicate
                ? Optional.of(renderDuplicateQuestionHandlingOptions(questionDefinition))
                : Optional.empty())
        .withCondData(
            questionIsRepeated,
            "enumerator",
            // Ternary operator to short-circuit, since otherwise `withCondData` would evaluate
            // a null expression
            questionIsRepeated
                ? questionsById.get(questionDefinition.getEnumeratorId().get()).getName()
                : "");
  }

  private static DivTag makeDuplicateQuestionBadge(String... classes) {
    return makeBadge(
        "Duplicate Question",
        new ImmutableList.Builder<String>().add(classes).add("bg-red-600").build());
  }

  private static DivTag makeNewQuestionBadge(String... classes) {
    return makeBadge(
        "New Question",
        new ImmutableList.Builder<String>().add(classes).add("bg-blue-600").build());
  }

  /**
   * Helper method for creating badges on question cards.
   *
   * @param text - text to display on the badge
   * @param classes - classes to add to the badge (must include background color)
   */
  private static DivTag makeBadge(String text, ImmutableList<String> classes) {
    return div()
        .withClasses(
            "rounded-lg",
            "flex",
            "max-w-fit",
            "px-2",
            "py-1",
            "space-x-1",
            "text-white",
            String.join(" ", classes))
        .with(span(text));
  }

  /**
   * Radio group for handling all duplicate questions. Renders with USWDS style. We prefer this to
   * {@link RadioButtonQuestionRenderer} for higher information density.
   */
  private static FieldsetTag renderToplevelDuplicateQuestionHandlingOptions() {
    return fieldset()
        .withName(AdminProgramImportForm.TOPLEVEL_DUPLICATE_QUESTION_HANDLING_FIELD)
        .withClasses("usa-fieldset", "my-4")
        .withData("testid", "toplevel-duplicate-handling")
        .with(
            legend("How do you want to handle all duplicate questions?")
                .withClasses("usa-legend", "font-semibold", "font-sans-xs"),
            div("Selecting an option for any individual question will override this selection")
                .withClasses("text-gray-500"),
            renderRadioOption(
                AdminProgramImportForm.TOPLEVEL_DUPLICATE_QUESTION_HANDLING_FIELD,
                "DECIDE_FOR_EACH",
                "Decide for each duplicate question individually",
                true),
            renderRadioOption(
                AdminProgramImportForm.TOPLEVEL_DUPLICATE_QUESTION_HANDLING_FIELD,
                "USE_EXISTING",
                "Use the existing questions",
                false),
            renderRadioOption(
                AdminProgramImportForm.TOPLEVEL_DUPLICATE_QUESTION_HANDLING_FIELD,
                "CREATE_DUPLICATE",
                "Create new duplicate questions",
                false),
            renderRadioOption(
                AdminProgramImportForm.TOPLEVEL_DUPLICATE_QUESTION_HANDLING_FIELD,
                "OVERWRITE_EXISTING",
                "Overwrite all instances of the existing questions",
                false));
  }

  /**
   * Radio group for handling a duplicate question. Renders with USWDS style. We prefer this to
   * {@link RadioButtonQuestionRenderer} for higher information density.
   *
   * @param question the question for which duplicate-handling options should be rendered
   * @return a radio group with options for how to handle the duplicate question.
   */
  private static FieldsetTag renderDuplicateQuestionHandlingOptions(QuestionDefinition question) {
    String adminName = question.getName();
    return fieldset()
        .withName(AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX)
        .withClasses("usa-fieldset", "shrink-0", "mb-4")
        .with(
            legend("How do you want to handle this duplicate question?")
                .withClasses("usa-legend", "font-semibold", "font-sans-xs"))
        .condWith(
            question.isEnumerator(),
            AlertComponent.renderSlimInfoAlert(
                "Duplicate repeated questions of this enumerator will also be set to 'Create a new"
                    + " duplicate question.'",
                /* hidden= */ true,
                /* classes...= */ "mb-2",
                "repeated-disabled-warning"))
        .condWith(
            question.isRepeated(),
            AlertComponent.renderSlimInfoAlert(
                "Some options are disabled because the associated enumerator is set to 'Create a"
                    + " new duplicate question.'",
                /* hidden= */ true,
                /* classes...= */ "mb-2",
                "repeated-disabled-warning"))
        .with(
            renderRadioOption(
                AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX + adminName,
                "USE_EXISTING",
                span("Use the ").with(renderExistingQuestionLink(adminName)),
                true),
            renderRadioOption(
                AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX + adminName,
                "CREATE_DUPLICATE",
                "Create a new duplicate question",
                false),
            renderRadioOption(
                AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX + adminName,
                "OVERWRITE_EXISTING",
                span("Overwrite all instances of the ").with(renderExistingQuestionLink(adminName)),
                false));
  }

  private static DomContent renderExistingQuestionLink(String adminName) {
    return new LinkElement()
        .setText("existing question")
        .setHref(routes.AdminQuestionController.index(Optional.of("Admin ID: " + adminName)).url())
        .opensInNewTab()
        .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
        .asAnchorText();
  }

  /**
   * Renders a radio option with USWDS style. We prefer this to {@link RadioButtonQuestionRenderer}
   * for higher information density.
   */
  private static DivTag renderRadioOption(
      String name, String value, DomContent text, boolean checked) {
    String id = RandomStringUtils.randomAlphabetic(8);

    InputTag inputTag =
        input()
            .withId(id)
            .withType("radio")
            .withName(name)
            .withValue(value)
            .withCondChecked(checked)
            .withClasses("usa-radio__input");

    LabelTag labelTag =
        label()
            .withFor(id)
            .withClasses("usa-radio__label", "inline-block", "w-full", "h-full", "font-sans-xs")
            .with(text);

    // A fully-styled USWDS radio would include the "usa-radio" class. However, we omit it to avoid
    // the default white background.
    return div().with(inputTag, labelTag);
  }

  private static DivTag renderRadioOption(String name, String value, String text, boolean checked) {
    return renderRadioOption(name, value, text(text), checked);
  }
}
