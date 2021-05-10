package views.admin.programs;

import static j2html.TagCreator.div;

import com.google.inject.Inject;
import forms.ProgramForm;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminView;
import views.components.ToastMessage;

public final class ProgramNewOneView extends AdminView {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request) {
    return render(request, new ProgramForm(), "");
  }

  public Content render(Request request, ProgramForm programForm, String message) {
    String title = "New program";

    ContainerTag contentDiv =
        div(
            ProgramFormBuilder.buildProgramForm(
                    programForm.getAdminName(),
                    programForm.getAdminDescription(),
                    programForm.getLocalizedDisplayName(),
                    programForm.getLocalizedDisplayDescription(),
                    false)
                .with(makeCsrfTokenInputTag(request))
                .withAction(controllers.admin.routes.AdminProgramController.create().url()));

    HtmlBundle htmlBundle = getHtmlBundle()
            .setTitle(title)
            .addMainContent(renderHeader(title), contentDiv);

    if (!message.isEmpty()) {
      htmlBundle.addToastMessages(ToastMessage.error(message).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }
}
