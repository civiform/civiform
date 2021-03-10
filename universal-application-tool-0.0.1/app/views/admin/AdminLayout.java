package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.head;
import static j2html.TagCreator.main;
import static j2html.TagCreator.nav;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseStyles;
import views.Styles;
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
        nav()
            .with(headerLink("Questions", questionLink))
            .with(headerLink("Programs", programLink))
            .with(headerLink("Logout", logoutLink, ImmutableList.of(Styles.FLOAT_RIGHT)))
            .withClasses(BaseStyles.NAV_STYLES);
    return htmlContent(
        head(tailwindStyles()),
        body()
            .with(adminHeader)
            .with(main(mainDomContents))
            .withClasses(
                Styles.BOX_BORDER,
                Styles.H_SCREEN,
                Styles.W_SCREEN,
                Styles.OVERFLOW_HIDDEN,
                Styles.FLEX));
  }

  public Tag headerLink(String text, String href) {
    return headerLink(text, href, ImmutableList.of());
  }

  public Tag headerLink(String text, String href, ImmutableList<String> styles) {
    return a(text).withHref(href).withClasses(Styles.PX_3, String.join(" ", styles));
  }
}
