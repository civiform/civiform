package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;

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
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
  }

  /**
   * Renders the import page, showing a file upload area to upload a JSON file. If {@code
   * programData} is present, then the program data will also be displayed.
   */
  public Content render(Http.Request request) {
    String title = "Import an existing program";
    DivTag contentDiv =
        div()
            .withClasses("pt-10", "px-20", "font-family-sans", "grid-col-8")
            .with(renderBackButton())
            .with(h1(title))
            .with(
                p("Import a program that exists in another environment into this environment.")
                    .withClass("my-2"))
            .with(hr().withClasses("usa-hr", "my-5"));

    contentDiv.with(createInstructionsSection());
    contentDiv.with(renderProgramDataRegion(request));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent createInstructionsSection() {
    return div()
        .with(
            h2("How to import a program"),
            ol().withClass("usa-process-list")
                .with(
                    createInstructionStep(
                        "Export your program from your other environment",
                        "Find your program and choose the “Export program” option from the overflow"
                            + " menu. Then either copy or download the JSON code."),
                    createInstructionStep(
                        "Paste the JSON code in the field below",
                        "Locate the JSON code in your browser or on your local device. Copy and"
                            + " paste the code into the field below."),
                    createInstructionStep(
                        "Preview program before saving",
                        "Review your program information, name and questions, for any errors. If"
                            + " something looks out of paste, you can delete and start the process"
                            + " over."),
                    createInstructionStep(
                        "Save your program",
                        "Click Save to save the program to this environment. You should now be able"
                            + " to view the program in your program list.")));
  }

  private DomContent createInstructionStep(String headerText, String paragraphText) {
    return li().withClass("usa-process-list__item")
        .with(
            h4(headerText).withClass("usa-process-list__heading"),
            p(paragraphText).withClass("margin-top-05"));
  }

  private DomContent renderProgramDataRegion(Http.Request request) {
    return div()
        .with(h2("Program import").withClass("mb-4"))
        .with(
            div(createUploadProgramJsonForm(request))
                .withId(AdminImportViewPartial.PROGRAM_DATA_ID));
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
            .setAttribute("rows", "5")
            .getTextareaTag();

    return div()
        .with(
            p("To import a program, copy the JSON file content and paste into the box below")
                .withClass("py-2"),
            form()
                .attr("hx-encoding", "multipart/form-data")
                .attr("hx-post", routes.AdminImportController.hxImportProgram().url())
                .attr("hx-target", "#" + AdminImportViewPartial.PROGRAM_DATA_ID)
                .attr("hx-swap", "outerHTML")
                .with(makeCsrfTokenInputTag(request), jsonInputElement)
                .with(
                    submitButton("Preview program")
                        .withClasses("usa-button", "usa-button--outline", "mb-5")));
  }

  private DivTag renderBackButton() {
    return div()
        .withClasses("flex", "items-center", "mb-5")
        .with(Icons.svg(Icons.ARROW_LEFT).withClasses("mr-2", "w-5", "h-5"))
        .with(
            a("Back to all programs")
                .withHref(routes.AdminProgramController.index().url())
                .withClass("usa-link"));
  }
}
