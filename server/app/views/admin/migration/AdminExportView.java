package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;

/** A view allowing admins to export a program into JSON format. */
public final class AdminExportView extends BaseHtmlView {

  private static final String COPY_BUTTON_ID = "copy-json-button";
  public static final String PROGRAM_JSON_ID = "json-preview";

  private final AdminLayout layout;

  @Inject
  public AdminExportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
  }

  /**
   * Renders the export page, showing a preview of the program JSON. Admins can choose to copy or
   * download the JSON.
   */
  public Content render(
      Http.Request request, ProgramDefinition program, String json, String adminName) {
    String title = "Export a program";
    DivTag contentDiv =
        div()
            .withClasses("pt-10", "px-20")
            .with(renderBackButton())
            .with(h1(title).withClass("mb-2"))
            .with(
                p(
                    "To export a program, either copy the JSON file content or download the JSON"
                        + " file to your local device."))
            .with(hr().withClasses("usa-hr", "my-5"));

    contentDiv.with(renderJsonPreviewRegion(request, json, adminName));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent renderJsonPreviewRegion(Http.Request request, String json, String adminName) {
    return div()
        .withId(PROGRAM_JSON_ID)
        .with(h2("JSON export for " + adminName))
        .with(
            form()
                .withMethod("POST")
                .with(makeCsrfTokenInputTag(request))
                .with(
                    div()
                        .with(
                            button("Copy JSON")
                                .withId(COPY_BUTTON_ID)
                                .withClasses("usa-button", "usa-button--outline", "mr-2"),
                            submitButton("Download JSON")
                                .withClasses("usa-button", "usa-button--outline"))
                        .withClasses("flex", "my-5"))
                .with(
                    FieldWithLabel.textArea()
                        // We set this to disabled to prevent admin from editing the json.
                        // Since disabled fields aren't included in the form body, we need to
                        // include a hidden field with the same data that will be included in
                        // the form body.
                        .setDisabled(true)
                        .setId("program-json")
                        .setValue(json)
                        .setAttribute("rows", "12")
                        .getTextareaTag())
                .with(
                    FieldWithLabel.textArea()
                        .setFieldName(AdminProgramExportForm.PROGRAM_JSON_FIELD)
                        .setValue(json)
                        .getTextareaTag()
                        .withClass("hidden"))
                .withAction(routes.AdminExportController.downloadJson(adminName).url()));
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
