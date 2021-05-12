package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
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
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request) {
    ProgramForm blankForm = new ProgramForm();
    return layout.render(
        body(
            renderHeader("New program"),
            div(
                ProgramFormBuilder.buildProgramForm(
                        blankForm.getAdminName(),
                        blankForm.getAdminDescription(),
                        blankForm.getLocalizedDisplayName(),
                        blankForm.getLocalizedDisplayDescription(),
                        false)
                    .with(makeCsrfTokenInputTag(request))
                    .withAction(controllers.admin.routes.AdminProgramController.create().url()))));
  }

  public Content render(Request request, ProgramForm programForm, String message) {
    ContainerTag bodyContent =
        body(
            renderHeader("New program"),
            div(
                ProgramFormBuilder.buildProgramForm(
                        programForm.getAdminName(),
                        programForm.getAdminDescription(),
                        programForm.getLocalizedDisplayName(),
                        programForm.getLocalizedDisplayDescription(),
                        false)
                    .with(makeCsrfTokenInputTag(request))
                    .withAction(controllers.admin.routes.AdminProgramController.create().url())));
    if (message.length() > 0) {
      bodyContent.with(ToastMessage.error(message).setDismissible(false).getContainerTag());
    }
    return layout.render(bodyContent);
  }
}
