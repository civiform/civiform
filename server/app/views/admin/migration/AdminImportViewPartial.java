package views.admin.migration;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.p;
import static views.ViewUtils.makeAlert;
import static views.style.BaseStyles.ALERT_ERROR;

import com.google.inject.Inject;
import controllers.admin.ProgramMigrationWrapper;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.mvc.Http;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import views.BaseHtmlView;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;

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
            makeAlert(
                /* text= */ errorMessage,
                /* hidden= */ false,
                /* title= */ Optional.of("Error processing JSON"),
                /* classes...= */ ALERT_ERROR));
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
                p("Admin name: " + program.adminName()));
    // TODO(#7087): If the imported program admin name matches an existing program admin name, we
    // should show some kind of error because admin names need to be unique.

    for (BlockDefinition block : program.blockDefinitions()) {
      programDiv.with(renderProgramBlock(block));
    }

    FormTag hiddenForm =
        form()
            .withMethod("POST")
            .with(makeCsrfTokenInputTag(request))
            .with(
                FieldWithLabel.textArea()
                    .setFieldName(AdminProgramExportForm.PROGRAM_JSON_FIELD)
                    .setValue(json)
                    .getTextareaTag()
                    .withClass("hidden"))
            .with(
                div()
                    .with(submitButton("Save Program").withClass(ButtonStyles.SOLID_BLUE))
                    .withClasses("flex"))
            // create a real route here
            .withAction(routes.AdminImportController.index().url());

    return programDiv.with(hiddenForm);
  }

  private DomContent renderProgramBlock(BlockDefinition block) {
    DivTag blockDiv =
        div()
            .withClasses("border", "border-gray-200", "p-2")
            .with(h4(block.name()), p(block.description()));
    // TODO(#7087): Display eligibility and visibility predicates.

    for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
      blockDiv.with(renderQuestion(question));
    }
    return blockDiv;
  }

  private DomContent renderQuestion(ProgramQuestionDefinition question) {
    return div()
        .withClasses("border", "border-gray-200", "p-2")
        .with(p("Question ID: " + question.id()));
    // TODO(#7087): Fetch and display all the question info, not just the ID.
  }
}
