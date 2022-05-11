package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import forms.ProgramForm;

import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.DivTag;

import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

/** Renders a page for editing the name and description of a program. */
public class ProgramEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request, ProgramDefinition program) {
    FormTag formTag =
        ProgramFormBuilder.buildProgramForm(program, /* editExistingProgram = */ true)
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(program.id()))
            .attr("action", controllers.admin.routes.AdminProgramController.update(program.id()).url());

    String title = String.format("Edit program: %s", program.adminName());

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(div(renderHeader(title)), div(formTag));

    return layout.renderCentered(htmlBundle);
  }

  public Content render(Request request, long id, ProgramForm program, String message) {
    FormTag formTag =
        ProgramFormBuilder.buildProgramForm(program, /* editExistingProgram = */ true)
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(id))
            .attr("action", controllers.admin.routes.AdminProgramController.update(id).url());

    String title = String.format("Edit program: %s", program.getAdminName());

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(div(renderHeader(title)), div(formTag));

    if (!message.isEmpty()) {
      htmlBundle.addToastMessages(ToastMessage.error(message).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag buildManageQuestionLink(long id) {
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
