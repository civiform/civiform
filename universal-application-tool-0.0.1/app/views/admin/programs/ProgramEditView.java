package views.admin.programs;

import com.google.inject.Inject;
import forms.ProgramForm;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

public class ProgramEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, ProgramDefinition program) {
    ContainerTag programForm =
        ProgramFormBuilder.buildProgramForm(
                program.adminName(),
                program.adminDescription(),
                program.getNameForDefaultLocale(),
                program.getDescriptionForDefaultLocale(),
                true);

    return render(request, id, program.id(), programForm, program.adminName(), "");
  }

  public Content render(Request request, long id, ProgramForm program, String message) {
    ContainerTag programForm =
        ProgramFormBuilder.buildProgramForm(
                program.getAdminName(),
                program.getAdminDescription(),
                program.getLocalizedDisplayName(),
                program.getLocalizedDisplayDescription(),
                true);

    return render(request, id, programForm, message);
  }

  private Content render(Request request, long id, ContainerTag programForm, String programName, String toastMessage) {
    programForm
      .with(makeCsrfTokenInputTag(request))
      .with(buildManageQuestionLink(id))
      .withAction(controllers.admin.routes.AdminProgramController.update(id).url());

    if (!toastMessage.isEmpty()) {
      programForm.with(ToastMessage.error(toastMessage).setDismissible(false).getContainerTag());
    }

    // TODO: Set relevant titles with i18n support.
    String title = String.format("Edit program: %s", programName);
    ContainerTag headerTag = renderHeader(title);
        HtmlBundle bundle = new HtmlBundle()
        .setTitle(title)
        .addHeaderContent(AdminView.renderNavBar())
        .addHeaderContent(headerTag);
        .addMainContent(programForm);
    return layout.render(bundle);
  }

  private ContainerTag buildManageQuestionLink(long id) {
    String manageQuestionLink =
        controllers.admin.routes.AdminProgramBlocksController.index(id).url();
    return new LinkElement()
        .setId("manage-questions-link")
        .setHref(manageQuestionLink)
        .setText("Manage Questions →")
        .setStyles(Styles.MX_4, Styles.FLOAT_RIGHT)
        .asAnchorText();
  }
}
