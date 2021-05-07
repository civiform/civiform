package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import forms.ProgramForm;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.ToastMessage;

public final class ProgramNewOneView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request) {
    ProgramForm blankForm = new ProgramForm();
    return render(request, blankForm, "");
  }

  public Content render(Request request, ProgramForm programForm, String message) {
    ContainerTag mainContent = div(
                ProgramFormBuilder.buildProgramForm(
                        programForm.getAdminName(),
                        programForm.getAdminDescription(),
                        programForm.getLocalizedDisplayName(),
                        programForm.getLocalizedDisplayDescription(),
                        false)
                    .with(makeCsrfTokenInputTag(request))
                    .withAction(controllers.admin.routes.AdminProgramController.create().url()));

    // TODO: Set relevant titles with i18n support.
    String title = "New Program";
    HtmlBundle bundle = new HtmlBundle()
        .setTitle(title)
        .addMainContent(renderHeader(title))
        .addMainContent(mainContent);

    if (message.length() > 0) {
      bundle.addHeaderContent(
        ToastMessage.error(message).setDismissible(false).getContainerTag()
      );
    }

    return layout.render(bundle);
  }
}
