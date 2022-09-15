package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import forms.ProgramForm;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

/** Renders a page for editing the name and description of a program. */
public final class ProgramEditView extends ProgramFormBuilder {
  private final AdminLayout layout;

  @Inject
  public ProgramEditView(AdminLayoutFactory layoutFactory, Config configuration) {
    super(configuration);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(Request request, ProgramDefinition program) {
    FormTag formTag =
        buildProgramForm(program, /* editExistingProgram = */ true)
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(program.id()))
            .withAction(controllers.admin.routes.AdminProgramController.update(program.id()).url());

    String title = String.format("Edit program: %s", program.localizedName().getDefault());

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(renderHeader(title), formTag);

    return layout.renderCentered(htmlBundle);
  }

  public Content render(
      Request request, long id, ProgramForm program, Optional<ToastMessage> message) {
    FormTag formTag =
        buildProgramForm(program, /* editExistingProgram = */ true)
            .with(makeCsrfTokenInputTag(request))
            .with(buildManageQuestionLink(id))
            .withAction(controllers.admin.routes.AdminProgramController.update(id).url());

    String title = String.format("Edit program: %s", program.getLocalizedDisplayName());

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(renderHeader(title), formTag);

    message.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  private ATag buildManageQuestionLink(long id) {
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
