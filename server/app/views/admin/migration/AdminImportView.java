package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import controllers.admin.AdminImportController;
import controllers.admin.ProgramMigration;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;

/**
 * A view allowing admins to import a JSON representation of a program and add that program to their
 * environment.
 */
public class AdminImportView extends BaseHtmlView {
  /**
   * Play Framework can only parse request bodies up to 100KB: See
   * https://www.playframework.com/documentation/2.4.x/ScalaBodyParsers#Max-content-length and
   * https://github.com/civiform/civiform/issues/816. So, set the max length to be slightly under.
   *
   * <p>Note that the HTML textarea element will automatically truncate the string to be the max
   * character length, which will likely result in JSON parsing errors. TODO(#7087): Make that
   * truncation obvious to admins.
   */
  private static final int MAX_TEXT_LENGTH = 100000;

  private final AdminLayout layout;

  @Inject
  public AdminImportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT);
  }

  /**
   * Renders the import page, showing a file upload area to upload a JSON file. If {@code
   * programData} is present, then the program data will also be displayed.
   */
  public Content render(Http.Request request) {
    String title = "Import a program";
    DivTag contentDiv =
        div()
            .withClasses("pt-10", "px-20")
            .with(h1(title))
            .with(
                p("This page allows you to import a program that exists in another environment"
                        + " (like staging) and easily add the program to this environment.")
                    .withClass("my-2"))
            .with(
                p("First, open the other environment and use the \"Export\" tab to download a JSON"
                        + " file that represents the existing program.")
                    .withClass("my-2"))
            .with(
                p("Then, copy the JSON file contents and paste them into the box below. The program"
                        + " information will be displayed below, and you can verify all the"
                        + " information before adding the program.")
                    .withClass("my-2"));

    contentDiv.with(createUploadProgramJsonForm(request));
    contentDiv.with(renderProgramDataRegion());

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent createUploadProgramJsonForm(Http.Request request) {
    DivTag jsonInputElement =
        FieldWithLabel.textArea()
            .setFieldName(AdminProgramImportForm.PROGRAM_JSON_FIELD)
            .setPlaceholderText("Paste the JSON file contents into this box.")
            // Note: The AdminExportView will pretty-prints the JSON, which adds a lot of
            // whitespace. If we find that admins are regularly going over the length limit, we
            // could stop pretty-printing the JSON.
            .setMaxLength(MAX_TEXT_LENGTH)
            .getTextareaTag();

    return div()
        .with(
            form()
                .attr("hx-encoding", "multipart/form-data")
                .attr("hx-post", routes.AdminImportController.hxImportProgram().url())
                .attr("hx-target", "#" + AdminImportViewPartial.PROGRAM_DATA_ID)
                .attr("hx-swap", "outerHTML")
                .with(makeCsrfTokenInputTag(request), jsonInputElement)
                .with(
                    submitButton("Display program information")
                        .withClasses(ButtonStyles.SOLID_BLUE, "mt-4")));
  }

  private DomContent renderProgramDataRegion() {
    return div()
        .withClass("mt-10")
        .with(h2("Uploaded program data").withClass("mb-4"))
        .with(
            div()
                .withId(AdminImportViewPartial.PROGRAM_DATA_ID)
                .with(p("No data has been uploaded yet.")));
  }

  private DomContent renderProgramMigration(ProgramMigration programMigration) {
    ProgramDefinition program = programMigration.getProgram();
    DivTag programDiv = div();
    programDiv.with(h3("Program name: " + program.localizedName().getDefault()));
    // TODO(#7087): If the imported program admin name matches an existing program admin name, we
    // should show some kind of error because admin names need to be unique.
    programDiv.with(p("Admin name: " + program.adminName()));

    for (BlockDefinition block : program.blockDefinitions()) {
      programDiv.with(renderProgramBlock(block));
    }
    return programDiv;
  }

  private DomContent renderProgramBlock(BlockDefinition block) {
    DivTag blockDiv = div().withClasses("border", "border-gray-200", "p-2");
    blockDiv.with(h4(block.name()));
    blockDiv.with(p(block.description()));

    for (ProgramQuestionDefinition questionDefinition : block.programQuestionDefinitions()) {
      DivTag questionDiv = div().withClasses("border", "border-gray-200", "p-2");
      questionDiv.with(p("Question ID: " + questionDefinition.id()));
      // TODO(#7087): Fetch and display all the question info, not just the ID.
      blockDiv.with(questionDiv);
    }
    return blockDiv;
  }
}
