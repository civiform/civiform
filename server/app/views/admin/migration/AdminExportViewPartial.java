package views.admin.migration;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.p;
import static j2html.TagCreator.button;
import static j2html.TagCreator.form;
import static views.ViewUtils.makeAlert;
import static j2html.TagCreator.textarea;
import static views.style.BaseStyles.ALERT_ERROR;

import com.google.inject.Inject;
import play.mvc.Http;
import controllers.admin.ProgramMigrationWrapper;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import views.BaseHtmlView;
import views.components.ButtonStyles;

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
  public DomContent renderProgramData(Http.Request request, String json) {
    // ProgramDefinition program = programMigrationWrapper.getProgram();

    // want this to be a textarea (or input?) with the json copied inside + a copy button
    // Two buttons -- copy json, download json


    // probably a form of type text area with one submit button (to download)
    // and one copy button that is controlled with js


    DivTag programDiv =
        div()
            .withId(PROGRAM_DATA_ID)
            .with(
                form()
                .withMethod("GET")
                .with(makeCsrfTokenInputTag(request))
                .with(textarea())
                // set json as input text
                .with(submitButton("Download Json").withClass(ButtonStyles.SOLID_BLUE))
            )
            .with(p(json))
            .with(button("Copy json").withClass(ButtonStyles.SOLID_BLUE))
            .with(button("Download json").withClass(ButtonStyles.SOLID_BLUE)).withClasses("mt-10", "mb-10");

    return programDiv;
  }
}
