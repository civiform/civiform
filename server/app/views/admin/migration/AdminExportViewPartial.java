package views.admin.migration;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import views.BaseHtmlView;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;

/** An HTMX partial for portions of the page rendered by {@link AdminExportView}. */
public final class AdminExportViewPartial extends BaseHtmlView {
  @Inject
  AdminExportViewPartial() {}

  /**
   * The ID for the div containing the program json preview. Must be applied to the top-level DOM
   * element of each partial so that replacement works correctly.
   */
  public static final String PROGRAM_DATA_ID = "json-preview";

  /** Renders the json preview section. */
  public DomContent renderProgramData(Http.Request request, String json, String adminName) {

    DivTag programDiv =
        div()
            .withId(PROGRAM_DATA_ID)
            .with(
                form()
                    .withMethod("POST")
                    .with(makeCsrfTokenInputTag(request))
                    .with(
                        FieldWithLabel.textArea()
                            // We set this to disabled to prevent admin from editing the json.
                            // Since disabled fields aren't included in the form body, we need to
                            // include a hidden field
                            // with the same data that will be included in the form body.
                            .setDisabled(true)
                            .setId("program-json")
                            .setValue(json)
                            .getTextareaTag())
                    .with(
                        FieldWithLabel.textArea()
                            .setFieldName(AdminProgramExportForm.PROGRAM_JSON_FIELD)
                            .setValue(json)
                            .getTextareaTag()
                            .withClass("hidden"))
                    .with(
                        div()
                            .with(
                                button("Copy Json")
                                    .withId("copy-json-button")
                                    .withClasses(ButtonStyles.SOLID_BLUE, "mr-2"),
                                submitButton("Download Json").withClass(ButtonStyles.SOLID_BLUE))
                            .withClasses("flex"))
                    .withAction(routes.AdminExportController.downloadJson(adminName).url()))
            .withClasses("mb-10");

    return programDiv;
  }
}
