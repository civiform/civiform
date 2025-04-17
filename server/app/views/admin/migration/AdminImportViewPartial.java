package views.admin.migration;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.UlTag;
import java.util.Objects;
import java.util.Optional;
import play.mvc.Http;
import services.AlertType;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.BaseHtmlView;
import views.ViewUtils;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.SvgTag;
import views.components.TextFormatter;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** An HTMX partial for portions of the page rendered by {@link AdminImportView}. */
public final class AdminImportViewPartial extends BaseHtmlView {
  private final SettingsManifest settingsManifest;

  @Inject
  AdminImportViewPartial(SettingsManifest settingsManifest) {
    this.settingsManifest = settingsManifest;
  }

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

  /** Renders the correctly parsed program data. */
  public DomContent renderProgramData(
      Http.Request request,
      ProgramDefinition program,
      ImmutableList<QuestionDefinition> questions,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap,
      String json,
      boolean withDuplicates) {

    boolean duplicateHandlingOptionsEnabled =
        settingsManifest.getImportDuplicateHandlingOptionsEnabled(request);

    ImmutableMap<String, String> newToOldQuestionNameMap =
        getNewToOldQuestionAdminNameMap(updatedQuestionsMap);

    DivTag questionAlert =
        buildQuestionAlert(
            updatedQuestionsMap,
            newToOldQuestionNameMap,
            withDuplicates,
            duplicateHandlingOptionsEnabled);

    DivTag programDiv =
        duplicateHandlingOptionsEnabled
            ? renderProgramDiv(program, questionAlert, updatedQuestionsMap)
            : renderProgramDivLegacyUi(program, questionAlert, updatedQuestionsMap);
    ImmutableMap<Long, QuestionDefinition> questionsById = ImmutableMap.of();

    if (withDuplicates && !updatedQuestionsMap.isEmpty()) {
      questionsById =
          updatedQuestionsMap.values().stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    // If there are no questions in the program, the "questions" field will not be included in the
    // // JSON and questions will be null here
    if (!withDuplicates && questions != null) {
      questionsById =
          questions.stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    for (BlockDefinition block : program.blockDefinitions()) {
      programDiv.with(
          renderProgramBlock(
              block,
              questionsById,
              newToOldQuestionNameMap,
              withDuplicates,
              duplicateHandlingOptionsEnabled));
    }

    FormTag hiddenForm =
        form()
            .attr("hx-encoding", "application/x-www-form-urlencoded")
            .attr("hx-post", routes.AdminImportController.hxSaveProgram().url())
            .attr("hx-target", "#" + AdminImportViewPartial.PROGRAM_DATA_ID)
            .attr("hx-swap", "outerHTML")
            .with(makeCsrfTokenInputTag(request))
            .with(
                FieldWithLabel.textArea()
                    .setFieldName(AdminProgramImportForm.PROGRAM_JSON_FIELD)
                    .setValue(json)
                    .getTextareaTag()
                    .withClass("hidden"))
            .with(
                div()
                    .with(
                        submitButton("Save").withClasses("usa-button", "mr-2"),
                        asRedirectElement(
                                button("Delete and start over"),
                                routes.AdminImportController.index().url())
                            .withClasses("usa-button", "usa-button--outline"))
                    .withClasses("flex", "my-5"))
            .withAction(routes.AdminImportController.hxSaveProgram().url());

    return programDiv.with(hiddenForm);
  }

  /** Renders a top-level program container. */
  private DivTag renderProgramDiv(
      ProgramDefinition program,
      DivTag questionAlert,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            h3("Program preview"),
            AlertComponent.renderSlimAlert(
                /* text= */ "Please review the program name and details before saving.",
                /* classes...= */ "mb-2"))
        .condWith(!updatedQuestionsMap.isEmpty(), questionAlert)
        .with(
            h2(program.localizedName().getDefault()).withClasses("mb-2", "font-semibold"),
            h4("Admin name: " + program.adminName()).withClass("mb-2"));
  }

  /**
   * Renders a top-level program container with the legacy UI (before duplicate handling options).
   */
  private DivTag renderProgramDivLegacyUi(
      ProgramDefinition program,
      DivTag questionAlert,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            h3("Program preview"),
            AlertComponent.renderFullAlert(
                /* text= */ "Please review the program name and details before saving.",
                /* classes...= */ "mb-2"))
        .condWith(!updatedQuestionsMap.isEmpty(), questionAlert)
        .with(
            h4("Program name: " + program.localizedName().getDefault()).withClass("mb-2"),
            h4("Admin name: " + program.adminName()).withClass("mb-2"));
  }

  /** Renders a message saying the program was successfully saved. */
  public DomContent renderProgramSaved(Http.Request request, String programName, Long programId) {
    String successText =
        programName
            + " and its questions have been imported to your program dashboard. To view it,  visit"
            + " the program dashboard.";
    if (settingsManifest.getNoDuplicateQuestionsForMigrationEnabled(request)) {
      successText += " Before you import another program, you will need to publish all drafts.";
    }
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
                        .withClasses("usa-button", "mr-2"))
                .condWith(
                    !settingsManifest.getNoDuplicateQuestionsForMigrationEnabled(request),
                    asRedirectElement(
                            button("Import another program"),
                            routes.AdminImportController.index().url())
                        .withClasses("usa-button", "usa-button--outline"))
                .withClasses("flex", "my-5"));
  }

  private ImmutableMap<String, String> getNewToOldQuestionAdminNameMap(
      ImmutableMap<String, QuestionDefinition> questions) {
    return questions.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getValue().getName(), entry -> entry.getKey()));
  }

  private DivTag buildQuestionAlert(
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap,
      ImmutableMap<String, String> newToOldQuestionNameMap,
      boolean withDuplicates,
      boolean withDuplicateHandlingOptions) {
    int numDuplicateQuestions = countDuplicateQuestions(newToOldQuestionNameMap);
    int numNewQuestions = updatedQuestionsMap.size() - numDuplicateQuestions;

    AlertType alertType = AlertType.INFO;
    String alertMessage = "";

    if (withDuplicates || numNewQuestions > 0) {
      alertMessage += "Importing this program will add ";
    }

    if (numNewQuestions > 0) {
      alertMessage += buildAlertWithNewQuestions(numNewQuestions);

      if (numDuplicateQuestions > 0) {
        alertType = AlertType.WARNING;
        alertMessage += withDuplicates ? " and " : " to the question bank. ";
      } else if (withDuplicates || numDuplicateQuestions == 0) {
        alertMessage += " to the question bank.";
      }
    }
    if (numDuplicateQuestions > 0) {
      alertType = AlertType.WARNING;
      if (withDuplicates) {
        alertMessage += addDuplicateMessageToAlert(numDuplicateQuestions);
      } else {
        alertMessage += addExistingMessageToAlert(numDuplicateQuestions);
      }
    }
    if (withDuplicateHandlingOptions) {
      return AlertComponent.renderSlimAlert(
          alertType, alertMessage, /* hidden= */ false, /* classes...= */ "mb-2");
    }
    return AlertComponent.renderFullAlert(
        alertType,
        alertMessage,
        /* title= */ Optional.empty(),
        /* hidden= */ false,
        /* classes...= */ "");
  }

  private String buildAlertWithNewQuestions(int numNewQuestions) {
    return String.format("%s new question%s", numNewQuestions, numNewQuestions > 1 ? "s" : "");
  }

  private String addDuplicateMessageToAlert(int numDuplicateQuestions) {
    return String.format(
        "%s duplicate question%s to the question bank.",
        numDuplicateQuestions, numDuplicateQuestions > 1 ? "s" : "");
  }

  private String addExistingMessageToAlert(int numExistingQuestions) {
    boolean plural = numExistingQuestions > 1;
    String makePlural = plural ? "s" : "";
    return String.format(
        "There %s %s existing question%s that will appear as draft%s in the question bank.",
        plural ? "are" : "is", numExistingQuestions, makePlural, makePlural);
  }

  private int countDuplicateQuestions(ImmutableMap<String, String> newToOldQuestionNameMap) {
    return newToOldQuestionNameMap.entrySet().stream()
        .filter(question -> !question.getKey().equals(question.getValue()))
        .collect(ImmutableList.toImmutableList())
        .size();
  }

  private DomContent renderProgramBlock(
      BlockDefinition block,
      ImmutableMap<Long, QuestionDefinition> questionsById,
      ImmutableMap<String, String> newToOldQuestionNameMap,
      boolean withDuplicates,
      boolean withDuplicateHandlingOptions) {
    DivTag blockDiv =
        withDuplicateHandlingOptions
            ? div()
                .withClasses("border-t", "border-gray-200", "pt-2")
                .with(h4(block.name()).withClasses("font-semibold"), p(block.description()))
            : div()
                .withClasses("border", "border-gray-200", "p-2")
                .with(h4(block.name()), p(block.description()));
    // TODO: #7087 - Display eligibility and visibility predicates.

    if (!questionsById.isEmpty()) {
      for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
        blockDiv.with(
            withDuplicateHandlingOptions
                ? renderQuestionCard(
                    Objects.requireNonNull(questionsById.get(question.id())),
                    newToOldQuestionNameMap)
                : renderQuestion(
                    Objects.requireNonNull(questionsById.get(question.id())),
                    newToOldQuestionNameMap,
                    withDuplicates));
      }
    }

    return blockDiv;
  }

  private DomContent renderQuestion(
      QuestionDefinition question,
      ImmutableMap<String, String> newToOldQuestionNameMap,
      boolean withDuplicates) {
    String currentAdminName = question.getName();
    boolean questionIsDuplicate =
        !currentAdminName.equals(newToOldQuestionNameMap.get(currentAdminName));

    String duplicateOrExistingText = withDuplicates ? "DUPLICATE QUESTION" : "EXISTING QUESTION";

    DivTag newOrDuplicateIndicator =
        questionIsDuplicate
            ? div(p(duplicateOrExistingText).withClass("p-2"))
                .withClasses("bg-yellow-100", "w-44", "flex", "justify-center")
            : div(p("NEW QUESTION").withClass("p-2"))
                .withClasses("bg-cyan-100", "w-32", "flex", "justify-center");
    DivTag questionDiv =
        div()
            .withClasses("p-2")
            .with(
                newOrDuplicateIndicator,
                div()
                    .with(
                        TextFormatter.formatTextForAdmins(question.getQuestionText().getDefault()))
                    .withClass("font-bold")
                    .withData("testid", "question-div"));
    if (!question.getQuestionHelpText().isEmpty()) {
      questionDiv.with(
          TextFormatter.formatTextForAdmins(question.getQuestionHelpText().getDefault()));
    }

    questionDiv.with(
        p("Admin name: " + question.getName()),
        p("Admin description: " + question.getDescription()),
        p("Question type: " + question.getQuestionType().name()));

    // If a question offers options, show them
    if (question.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
      UlTag optionList = ul().withClasses("list-disc", "ml-10");
      for (QuestionOption option : multiOption.getOptions()) {
        optionList.with(li(option.optionText().getDefault()));
      }
      questionDiv.with(optionList);
    }

    return questionDiv;
  }

  /**
   * Renders an individual question, including the admin ID, help text, and any options or tags that
   * should be shown next to the question in the list of questions.
   */
  private DivTag renderQuestionCard(
      QuestionDefinition questionDefinition, ImmutableMap<String, String> newToOldQuestionNameMap) {
    String currentAdminName = questionDefinition.getName();
    boolean questionIsUniversal = questionDefinition.isUniversal();
    boolean questionIsDuplicate =
        !currentAdminName.equals(newToOldQuestionNameMap.get(currentAdminName));

    // TODO: #9628 - The classes below are copied from the Question Card-rendering method in
    // ProgramBlockView. We should consider factoring out the card rendering logic to a common
    // helper method to limit the potential for diverging styles.
    DivTag cardDiv =
        div()
            .withData("testid", "question-admin-name-" + currentAdminName)
            .withClasses(
                ReferenceClasses.PROGRAM_QUESTION,
                "my-2",
                "px-4",
                "py-2",
                "items-center",
                "rounded-md",
                "border",
                StyleUtils.hover("text-gray-800", "bg-gray-100"))
            .with(
                div()
                    .condWith(
                        questionIsUniversal,
                        ViewUtils.makeUniversalBadge(questionDefinition, "mr-2"))
                    .with(
                        questionIsDuplicate ? makeDuplicateQuestionBadge() : makeNewQuestionBadge())
                    .withClasses("flex", "mt-2", "mb-4"));

    SvgTag icon =
        Icons.questionTypeSvg(questionDefinition.getQuestionType())
            .withClasses("shrink-0", "h-12", "w-6");
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();

    DivTag content =
        div()
            .withClass("flex-grow")
            .with(
                div()
                    .with(
                        TextFormatter.formatTextForAdmins(
                            questionDefinition.getQuestionText().getDefault()))
                    .withData("testid", "question-div"),
                div()
                    .with(TextFormatter.formatTextForAdmins(questionHelpText))
                    .withClasses("mt-1", "text-sm"),
                p(String.format("Admin ID: %s", currentAdminName)).withClasses("mt-1", "text-sm"));
    content.condWith(
        questionDefinition.getQuestionType().isMultiOptionType(),
        questionDefinition.getQuestionType().isMultiOptionType()
            ? getOptions((MultiOptionQuestionDefinition) questionDefinition)
            : null);

    DivTag row = div().withClasses("flex", "gap-4", "items-center").with(icon, content);
    return cardDiv.with(row);
  }

  private static UlTag getOptions(MultiOptionQuestionDefinition question) {
    UlTag options = ul().withClasses("list-disc", "mx-4", "mt-2");
    for (QuestionOption option : question.getOptions()) {
      options.with(li(option.optionText().getDefault()));
    }
    return options;
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
}
