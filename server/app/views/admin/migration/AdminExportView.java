package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;

/** A view allowing admins to export a program into JSON format. */
public final class AdminExportView extends BaseHtmlView {

  public static final String PROGRAM_EXPORT_FORM_ID = "program-export-form";
  public static final String GENERATE_JSON_BUTTON_ID = "generate-json-button";

  private final AdminLayout layout;

  @Inject
  public AdminExportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.EXPORT);
  }

  /**
   * Renders the export page, showing a list of all active programs. Admins can select a single
   * program then download it.
   */
  public Content render(Http.Request request, ImmutableList<ProgramDefinition> programs) {
    String title = "Export a program";
    DivTag contentDiv =
        div()
            .withClasses("pt-10", "px-20")
            .with(h1(title))
            .with(
                p("Select the program you'd like to export to a different environment and then"
                      + " click \"Generate JSON\". This will generate a JSON file representing the"
                      + " selected program.")
                    .withClass("my-2"))
            .with(
                p("Once the JSON file is generated, you can copy it to the clipboard or download a"
                      + " file containing the JSON. To import the JSON, open the environment where"
                      + " this program should be added, log in as a CiviForm Admin and use the"
                      + " \"Import\" tab to add the program.")
                    .withClass("my-2"));

    contentDiv.with(createProgramSelectionForm(request, programs));
    contentDiv.with(renderJSONPreviewRegion());

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent createProgramSelectionForm(
      Http.Request request, ImmutableList<ProgramDefinition> programs) {
    FieldsetTag fields = fieldset();
    for (ProgramDefinition program : programs) {
      String labelText =
          "Name: "
              + program.localizedName().getDefault()
              + "\n"
              + "Admin Name: "
              + program.adminName();

      fields
          .with(
              FieldWithLabel.radio()
                  .setFieldName(AdminProgramExportForm.PROGRAM_ID_FIELD)
                  .setLabelText(labelText)
                  .setValue(String.valueOf(program.id()))
                  .getRadioTag())
          .withClass("whitespace-pre-wrap");
    }

    return div()
        .with(
            form()
                .withId(PROGRAM_EXPORT_FORM_ID)
                .attr("hx-encoding", "multipart/form-data")
                .attr("hx-post", routes.AdminExportController.hxExportProgram().url())
                .attr("hx-target", "#" + AdminExportViewPartial.PROGRAM_JSON_ID)
                .attr("hx-swap", "outerHTML")
                .with(makeCsrfTokenInputTag(request))
                .with(fields)
                .with(
                    submitButton("Generate JSON")
                        .withId(GENERATE_JSON_BUTTON_ID)
                        .isDisabled()
                        .withClasses(ButtonStyles.SOLID_BLUE, "mb-10")));
  }

  private DomContent renderJSONPreviewRegion() {
    return div().withId(AdminExportViewPartial.PROGRAM_JSON_ID);
  }
}
