package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.OptionalInt;
import play.mvc.Http;
import play.twirl.api.Content;
import services.CiviFormError;
import services.ErrorAnd;
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
  private final AdminLayout layout;

  @Inject
  public AdminImportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT);
  }

  /**
   * Renders the import page, showing a file upload area to upload a JSON file. If {@code
   * programData} is present, then the program data will also be displayed.
   *
   * @param programData the program data that was parsed from the uploaded JSON file, including any
   *     errors that may have happened while parsing. If empty, no file has been uploaded yet.
   */
  public Content render(
      Http.Request request, Optional<ErrorAnd<String, CiviFormError>> programData) {
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

    // Note: FieldWithLabel automatically applies a maxlength of 10000 on text areas. We may need to
    // lift that restriction if program data is long
    DivTag jsonInputElement =
        FieldWithLabel.textArea()
            .setFieldName(AdminProgramImportForm.PROGRAM_JSON_FIELD)
            //   .setLabelText("Program JSON")
            .setPlaceholderText("Paste the JSON file contents into this box.")
            // The prettyprint of ComprehensiveSampleProgram is 27k characters, so programs could
            // pretty quickly exceed this
            // But we could also trim whitespace
            .setMaxlength(OptionalInt.of(65000000))
            .getTextareaTag();

    //  TextareaTag jsonInputElement =
    // textarea().withName(AdminProgramImportForm.PROGRAM_JSON_FIELD);

    return div()
        .with(
            form()
                .attr("hx-encoding", "multipart/form-data")
                .attr("hx-post", routes.AdminImportController.hxImportProgram().url())
                .attr("hx-target", "#program-data")
                .attr("hx-swap", "outerHTML")
                .with(makeCsrfTokenInputTag(request), jsonInputElement)
                .with(
                    submitButton("Display program information")
                        .withClass(ButtonStyles.SOLID_BLUE)));
  }

  private DomContent renderProgramDataRegion() {
    return div()
        .withClass("mt-10")
        .with(h2("Uploaded program data"))
        .with(
            div()
                .withId(AdminImportViewPartial.PROGRAM_DATA_ID)
                .with(p("No data has been uploaded yet.")));
  }
}
