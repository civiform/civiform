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
  private final AdminLayout layout;

  @Inject
  public AdminExportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.EXPORT);
  }

  /**
   * Renders the export page, showing a list of all active programs. Admins can select a single
   * program then download it.
   */
  public Content render(Http.Request request, ImmutableList<ProgramDefinition> activePrograms) {
    String title = "Export a program";
    DivTag contentDiv =
        div()
            .withClasses("pt-10", "px-20")
            .with(h1(title))
            .with(
                p("Select the program you'd like to export to a different environment and then"
                        + " click \"Download program\". This will download a JSON file representing"
                        + " the selected program.")
                    .withClass("my-2"))
            .with(
                p("Once the JSON file is downloaded, open the environment where this program should"
                        + " be added. Log in as a CiviForm Admin and use the \"Import\" tab to add"
                        + " the program.")
                    .withClass("my-2"));

    contentDiv.with(createProgramSelectionForm(request, activePrograms));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent createProgramSelectionForm(
      Http.Request request, ImmutableList<ProgramDefinition> activePrograms) {
    FieldsetTag fields = fieldset();
    for (ProgramDefinition program : activePrograms) {
      fields.with(
          FieldWithLabel.radio()
              .setFieldName(AdminProgramExportForm.PROGRAM_ID_FIELD)
              // TODO(#7087): Should we display the admin name, localized name, or both?
              .setLabelText(program.adminName())
              .setValue(String.valueOf(program.id()))
              .getRadioTag());
    }

    return div()
        .with(
            form()
                .withMethod("GET")
                .withAction(routes.AdminExportController.exportProgram().url())
                .with(makeCsrfTokenInputTag(request), fields)
                .with(submitButton("Download program").withClass(ButtonStyles.SOLID_BLUE)));
  }
}
