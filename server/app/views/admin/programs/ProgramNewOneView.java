package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import forms.ProgramForm;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.ToastMessage;

/** Renders a page for adding a new program. */
public final class ProgramNewOneView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request) {
    return render(request, new ProgramForm(), "");
  }

  public Content render(Request request, ProgramForm programForm, String message) {
    String title = "New program information";

    ContainerTag contentDiv =
        div(
            ProgramFormBuilder.buildProgramForm(programForm, /* editExistingProgram = */ false)
                .with(makeCsrfTokenInputTag(request))
                .withAction(controllers.admin.routes.AdminProgramController.create().url()));

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(renderHeader(title), contentDiv);

    if (!message.isEmpty()) {
      htmlBundle.addToastMessages(ToastMessage.error(message).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }
}
