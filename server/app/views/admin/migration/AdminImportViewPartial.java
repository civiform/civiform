package views.admin.migration;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
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
import views.components.FieldWithLabel;
import views.components.TextFormatter;

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

    ImmutableMap<String, String> newToOldQuestionNameMap =
        getNewToOldQuestionAdminNameMap(updatedQuestionsMap);

    DivTag questionAlert =
        buildQuestionAlert(updatedQuestionsMap, newToOldQuestionNameMap, withDuplicates);

    DivTag programDiv =
        div()
            .withId(PROGRAM_DATA_ID)
            .with(
                h3("Program preview"),
                AlertComponent.renderFullAlert(
                    AlertType.INFO,
                    /* text= */ "Please review the program name and details before saving.",
                    /* title= */ Optional.empty(),
                    /* hidden= */ false,
                    /* classes...= */ "mb-2"))
            .condWith(!updatedQuestionsMap.isEmpty(), questionAlert)
            .with(
                h4("Program name: " + program.localizedName().getDefault()).withClass("mb-2"),
                h4("Admin name: " + program.adminName()).withClass("mb-2"));

    ImmutableMap<Long, QuestionDefinition> questionsById = ImmutableMap.of();

    if (withDuplicates && !updatedQuestionsMap.isEmpty()) {
      questionsById =
          updatedQuestionsMap.values().stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    // If there are no questions in the program, the "questions" field will not be included in the
    // JSON and programMigrationWrapper.getQuestions() will return null
    if (!withDuplicates && questions != null) {
      questionsById =
          questions.stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    for (BlockDefinition block : program.blockDefinitions()) {
      programDiv.with(
          renderProgramBlock(
              block, questionsById, newToOldQuestionNameMap, /* withDuplicates= */ withDuplicates));
    }

    FormTag hiddenForm =
        form()
            .attr("hx-encoding", "multipart/form-data")
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
                            routes.AdminProgramBlocksController.edit(programId, 1).url())
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
      boolean withDuplicates) {
    int numDuplicateQuestions = countDuplicateQuestions(newToOldQuestionNameMap);
    int numNewQuestions = updatedQuestionsMap.size() - numDuplicateQuestions;

    AlertType alertType = AlertType.INFO;
    String alertMessage = "Importing this program will add ";

    if (numDuplicateQuestions > 0) {
      alertType = AlertType.WARNING;
      if (numNewQuestions > 0) {
        String questionOrQuestions = numNewQuestions == 1 ? "question" : "questions";
        alertMessage =
            alertMessage.concat(numNewQuestions + " new " + questionOrQuestions + " and ");
      }
      String questionOrQuestions = numDuplicateQuestions == 1 ? "question" : "questions";
      if (withDuplicates) {
        alertMessage =
            alertMessage.concat(
                numDuplicateQuestions
                    + " duplicate "
                    + questionOrQuestions
                    + " to the question bank.");
      } else {
        alertMessage =
            alertMessage.concat(
                "There are "
                    + numDuplicateQuestions
                    + " existing "
                    + questionOrQuestions
                    + " that will appear as drafts in the question bank.");
      }
    } else if (numNewQuestions > 0) {
      String questionOrQuestions = numNewQuestions == 1 ? "question" : "questions";
      alertMessage =
          alertMessage.concat(
              numNewQuestions + " new " + questionOrQuestions + " to the question bank.");
    }

    return AlertComponent.renderFullAlert(alertType, alertMessage, Optional.empty(), false, "");
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
      boolean withDuplicates) {
    DivTag blockDiv =
        div()
            .withClasses("border", "border-gray-200", "p-2")
            .with(h4(block.name()), p(block.description()));
    // TODO(#7087): Display eligibility and visibility predicates.

    if (!questionsById.isEmpty()) {
      for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
        blockDiv.with(
            renderQuestion(
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
                        TextFormatter.formatText(
                            question.getQuestionText().getDefault(), false, false))
                    .withClass("font-bold")
                    .withData("testid", "question-div"));
    if (!question.getQuestionHelpText().isEmpty()) {
      questionDiv.with(
          TextFormatter.formatText(question.getQuestionHelpText().getDefault(), false, false));
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
}
