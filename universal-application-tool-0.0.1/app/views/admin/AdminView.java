package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class AdminView extends BaseHtmlView {

  protected HtmlBundle getHtmlBundle() {
    return new HtmlBundle().addHeaderContent(renderNavBar());
  }

  private ContainerTag renderNavBar() {
    String questionLink = routes.QuestionController.index().url();
    String programLink = routes.AdminProgramController.index().url();
    String versionLink = routes.AdminVersionController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();

    // The CiviForm title/branding. This doesn't need to be translated.
    ContainerTag headerIcon =
        div(span("C"), span("F").withClasses(Styles.FONT_THIN))
            .withClasses(BaseStyles.ADMIN_NAV_BAR);
    ContainerTag headerTitle =
        div()
            .withClasses(
                Styles.FONT_NORMAL, Styles.INLINE, Styles.PL_10, Styles.PY_0, Styles.TEXT_XL)
            .with(span("Civi"), span("Form").withClasses(Styles.FONT_THIN));

    // TODO[i18n].
    ContainerTag adminHeader =
        nav()
            .with(headerIcon, headerTitle)
            .with(headerLink("Questions", questionLink))
            .with(headerLink("Programs", programLink))
            .with(headerLink("Versions", versionLink))
            .with(headerLink("Intermediaries", intermediaryLink))
            .with(headerLink("Logout", logoutLink, Styles.FLOAT_RIGHT))
            .withClasses(BaseStyles.NAV_STYLES);
    return adminHeader;
  }

  private Tag headerLink(String text, String href, String... styles) {
    return a(text)
        .withHref(href)
        .withClasses(
            Styles.PX_3,
            Styles.OPACITY_75,
            StyleUtils.hover(Styles.OPACITY_100),
            StyleUtils.joinStyles(styles));
  }
}
