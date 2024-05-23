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

/** An HTMX partial for portions of the page rendered by {@link AdminImportView}. */
public final class AdminExportViewPartial extends BaseHtmlView {
  @Inject
  AdminExportViewPartial() {}

  /**
   * The ID for the div containing the imported program data. Must be applied to the top-level DOM
   * element of each partial so that replacement works correctly.
   */
  public static final String PROGRAM_DATA_ID = "program-data";

  /** Renders an error that occurred while trying to parse the program data. */
  //   public DomContent renderError(String errorMessage) {
  //     return div()
  //         .withId(PROGRAM_DATA_ID)
  //         .with(
  //             makeAlert(
  //                 /* text= */ errorMessage,
  //                 /* hidden= */ false,
  //                 /* title= */ Optional.of("Error processing JSON"),
  //                 /* classes...= */ ALERT_ERROR));
  //   }

  /** Renders the correctly parsed program data. */
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
                            // we want to set this to disabled to preven the admin from editing the
                            // json
                            // but disabled fields aren't included in the form body
                            // so we need to include a hidden field that the user won't see
                            // but that will be included in the form body.
                            .setDisabled(true)
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
                                button("Copy Json").withClasses(ButtonStyles.SOLID_BLUE, "mr-2"),
                                submitButton("Download Json").withClass(ButtonStyles.SOLID_BLUE))
                            .withClasses("flex"))
                    .withAction(routes.AdminExportController.downloadJson(adminName).url()))
            .withClasses("mt-10", "mb-10");

    return programDiv;
  }
}
