package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;

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
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request, ProgramDefinition program) {
    ContainerTag formTag =
        ProgramFormBuilder.buildProgramForm(
                program.adminName(),
                program.adminDescription(),
                program.localizedName().getDefault(),
                program.localizedDescription().getDefault(),
                true)
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(program.id()))
            .withAction(controllers.admin.routes.AdminProgramController.update(program.id()).url());
    return layout.render(
        renderHeader(String.format("Edit program: %s", program.adminName())), formTag);
  }

  public Content render(Request request, long id, ProgramForm program, String message) {
    ContainerTag formTag =
        ProgramFormBuilder.buildProgramForm(
                program.getAdminName(),
                program.getAdminDescription(),
                program.getLocalizedDisplayName(),
                program.getLocalizedDisplayDescription(),
                true)
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(id))
            .withAction(controllers.admin.routes.AdminProgramController.update(id).url());

    if (!message.isEmpty()) {
      formTag.with(ToastMessage.error(message).setDismissible(false).getContainerTag());
    }

    return layout.render(
        renderHeader(String.format("Edit program: %s", program.getAdminName())), formTag);
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
