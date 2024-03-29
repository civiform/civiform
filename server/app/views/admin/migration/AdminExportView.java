package views.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

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

public final class AdminExportView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public AdminExportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.EXPORT);
  }

  public Content render(Http.Request request, ImmutableList<ProgramDefinition> activePrograms) {
    String title = "Export programs";
    DivTag contentDiv = div().with(h1(title));

    contentDiv.with(programSelection(request, activePrograms));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent programSelection(
      Http.Request request, ImmutableList<ProgramDefinition> activePrograms) {
    DivTag mainDiv = div();
    mainDiv.with(h2("Programs"));

    FieldsetTag fields = fieldset();

    for (ProgramDefinition program : activePrograms) {
      System.out.println(program.localizedName().getDefault() + "  is ID=" + program.id());
      fields.with(
          FieldWithLabel.radio()
              .setFieldName(AdminProgramExportForm.PROGRAM_ID_FIELD)
              .setLabelText(
                  String.format(
                      "%s (Admin name: %s)",
                      program.localizedName().getDefault(), program.adminName()))
              .setValue(String.valueOf(program.id()))
              .getRadioTag());
    }

    return mainDiv.with(
        form()
            .withMethod("GET")
            .withAction(routes.AdminExportController.exportPrograms().url())
            .with(makeCsrfTokenInputTag(request), fields)
            .with(submitButton("Export program").withClass(ButtonStyles.SOLID_BLUE)));
  }
}
