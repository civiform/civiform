package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.main;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.ViewUtils;

public class AdminLayout extends BaseHtmlLayout {

  @Inject
  public AdminLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  /** Renders mainDomContents within the main tag, in the context of the admin layout. */
  public Content render(DomContent... mainDomContents) {
    String questionLink = controllers.admin.routes.QuestionController.index("table").url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    ContainerTag adminHeader =
        header()
            .with(a("Questions").withHref(questionLink))
            .with(a("Programs").withHref(programLink))
            .with(a("Logout").withHref(logoutLink));
    return htmlContent(head(tailwindStyles()), body(adminHeader, main(mainDomContents)));
  }
}
