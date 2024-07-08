package views.admin.migration;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.p;
import static views.ViewUtils.makeAlert;
import static views.style.BaseStyles.ALERT_ERROR;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import controllers.admin.ProgramMigrationWrapper;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Objects;
import java.util.Optional;
import play.mvc.Http;
import services.AlertType;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.AlertComponent;
import views.BaseHtmlView;
import views.components.ButtonStyles;
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
  public DomContent renderError(String errorMessage) {
    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            AlertComponent.renderFullAlert(
                AlertType.ERROR,
                /* text= */ errorMessage,
                /* title= */ Optional.of("Error processing JSON"),
                /* hidden= */ false

                /* classes...= */ ));
  }

  /** Renders the correctly parsed program data. */
  public DomContent renderProgramData(
      Http.Request request, ProgramMigrationWrapper programMigrationWrapper, String json) {
    ProgramDefinition program = programMigrationWrapper.getProgram();
    DivTag programDiv =
        div()
            .withId(PROGRAM_DATA_ID)
            .with(
                h3("Program name: " + program.localizedName().getDefault()),
                h4("Admin name: " + program.adminName()));
    // TODO(#7087): If the imported program admin name matches an existing program admin name, we
    // should show some kind of error because admin names need to be unique.

    ImmutableMap<Long, QuestionDefinition> questionsById = ImmutableMap.of();
    // If there are no questions in the program, the "questions" field will not be included in the
    // JSON and programMigrationWrapper.getQuestions() will return null
    if (programMigrationWrapper.getQuestions() != null) {
      questionsById =
          programMigrationWrapper.getQuestions().stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
    }

    for (BlockDefinition block : program.blockDefinitions()) {
      programDiv.with(renderProgramBlock(block, questionsById));
    }

    FormTag hiddenForm =
        form()
            .withMethod("POST")
            .with(makeCsrfTokenInputTag(request))
            .with(
                FieldWithLabel.textArea()
                    .setFieldName(AdminProgramImportForm.PROGRAM_JSON_FIELD)
                    .setValue(json)
                    .getTextareaTag()
                    .withClass("hidden"))
            .with(
                div()
                    .with(submitButton("Save Program").withClass(ButtonStyles.SOLID_BLUE))
                    .withClasses("flex"))
            .withAction(routes.AdminImportController.saveProgram().url());

    return programDiv.with(hiddenForm);
  }

  private DomContent renderProgramBlock(
      BlockDefinition block, ImmutableMap<Long, QuestionDefinition> questionsById) {
    DivTag blockDiv =
        div()
            .withClasses("border", "border-gray-200", "p-2")
            .with(h4(block.name()), p(block.description()));
    // TODO(#7087): Display eligibility and visibility predicates.

    if (!questionsById.isEmpty()) {
      for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
        blockDiv.with(renderQuestion(Objects.requireNonNull(questionsById.get(question.id()))));
      }
    }

    return blockDiv;
  }

  private DomContent renderQuestion(QuestionDefinition question) {
    DivTag questionDiv =
        div()
            .withClasses("border", "border-gray-200", "p-2")
            .with(
                div()
                    .with(
                        TextFormatter.formatText(
                            question.getQuestionText().getDefault(), false, false))
                    .withClass("font-bold")
                    .withData("testid", "question-div"));
    // if (!question.getQuestionHelpText().isEmpty()) {
    //   questionDiv.with(
    //       TextFormatter.formatText(question.getQuestionHelpText().getDefault(), false, false));
    // }

    questionDiv.with(p("Admin name: " + question.getName()));
    // p("Admin description: " + question.getDescription()),
    // p("Question type: " + question.getQuestionType().name()));

    // If a question offers options, show them
    // if (question.getQuestionType().isMultiOptionType()) {
    //   MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
    //   UlTag optionList = ul().withClasses("list-disc", "ml-10");
    //   for (QuestionOption option : multiOption.getOptions()) {
    //     optionList.with(li(option.optionText().getDefault()));
    //   }
    //   questionDiv.with(optionList);
    // }

    return questionDiv;
  }
}
