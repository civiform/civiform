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
import views.AlertComponent;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.components.TextFormatter;

/** An HTMX partial for portions of the page rendered by {@link AdminImportView}. */
public final class AdminImportViewPartial extends BaseHtmlView {
  @Inject
  AdminImportViewPartial() {}

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
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap,
      String json) {

    ImmutableMap<String, String> newToOldQuestionNameMap =
        getNewToOldQuestionAdminNameMap(updatedQuestionsMap);

    int numDuplicateQuestions = countDuplicateQuestions(newToOldQuestionNameMap);
    int numNewQuestions = updatedQuestionsMap.size() - numDuplicateQuestions;

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
            .with(
                AlertComponent.renderFullAlert(
                    AlertType.WARNING,
                    "Importing this program will add "
                        + numNewQuestions
                        + " new questions and "
                        + numDuplicateQuestions
                        + " duplicate questions to the question bank.",
                    Optional.empty(),
                    false,
                    ""))
            .with(
                h4("Program name: " + program.localizedName().getDefault()).withClass("mb-2"),
                h4("Admin name: " + program.adminName()).withClass("mb-2"));

    ImmutableMap<Long, QuestionDefinition> questionsById = ImmutableMap.of();
    // If there are no questions in the program, the "questions" field will not be included in the
    // JSON and programMigrationWrapper.getQuestions() will return null
    if (updatedQuestionsMap != null) {
      questionsById =
          updatedQuestionsMap.values().stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    for (BlockDefinition block : program.blockDefinitions()) {
      programDiv.with(renderProgramBlock(block, questionsById, newToOldQuestionNameMap));
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
  public DomContent renderProgramSaved(String programName, Long programId) {
    return div()
        .with(
            AlertComponent.renderFullAlert(
                AlertType.SUCCESS,
                /* text= */ programName
                    + " and its questions have been imported to your program dashboard. To view it,"
                    + " visit the program dashboard.",
                /* title= */ Optional.of("Your program has been successfully imported"),
                /* hidden= */ false,
                /* classes...= */ "mb-2"),
            div()
                .with(
                    asRedirectElement(
                            button("View program"),
                            routes.AdminProgramBlocksController.edit(programId, 1).url())
                        .withClasses("usa-button", "mr-2"),
                    asRedirectElement(
                            button("Import another program"),
                            routes.AdminImportController.index().url())
                        .withClasses("usa-button", "usa-button--outline"))
                .withClasses("flex", "my-5"));
  }

  private DomContent renderProgramBlock(
      BlockDefinition block,
      ImmutableMap<Long, QuestionDefinition> questionsById,
      ImmutableMap<String, String> newToOldQuestionNameMap) {
    DivTag blockDiv =
        div()
            .withClasses("border", "border-gray-200", "p-2")
            .with(h4(block.name()), p(block.description()));
    // TODO(#7087): Display eligibility and visibility predicates.

    if (!questionsById.isEmpty()) {
      for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
        blockDiv.with(
            renderQuestion(
                Objects.requireNonNull(questionsById.get(question.id())), newToOldQuestionNameMap));
      }
    }

    return blockDiv;
  }

  private DomContent renderQuestion(
      QuestionDefinition question, ImmutableMap<String, String> newToOldQuestionNameMap) {
    String currentAdminName = question.getName();
    boolean questionIsDuplicate =
        !currentAdminName.equals(newToOldQuestionNameMap.get(currentAdminName));

    System.out.println(question.getName());
    System.out.println(newToOldQuestionNameMap);
    DivTag newOrDuplicateIndicator =
        questionIsDuplicate
            ? div(p("DUPLICATE QUESTION").withClass("p-2")).withClass("bg-yellow-100")
            : div(p("NEW QUESTION").withClass("p-2")).withClass("bg-cyan-100");
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

  private ImmutableMap<String, String> getNewToOldQuestionAdminNameMap(
      ImmutableMap<String, QuestionDefinition> questions) {
    return questions.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getValue().getName(), entry -> entry.getKey()));
  }

  private int countDuplicateQuestions(ImmutableMap<String, String> newToOldQuestionNameMap) {
    return newToOldQuestionNameMap.entrySet().stream()
        .filter(question -> !question.getKey().equals(question.getValue()))
        .collect(ImmutableList.toImmutableList())
        .size();
  }
}
