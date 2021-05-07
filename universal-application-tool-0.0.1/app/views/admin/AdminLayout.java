package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.head;
import static j2html.TagCreator.main;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.ViewUtils;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class AdminLayout extends BaseHtmlLayout {

  @Inject
  public AdminLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }
  String MAIN_STYLES = StyleUtils.joinStyles(Styles.BG_WHITE, Styles.BORDER, Styles.BORDER_GRAY_200, Styles.MT_12, Styles.OVERFLOW_Y_AUTO,
        Styles.SHADOW_LG, Styles.W_SCREEN);
  String CENTERED_STYLES = StylesUitls.joinStyles(Styles.PX_2, Styles.MAX_W_SCREEN_XL, Styles.MX_AUTO);
  String FULL_STYLES = StyleUtils.joinStyles(Styles.FLEX, Styles.FLEX_ROW);
  String BODY_STYLES = StyleUtils.joinStyles(BaseStyles.BODY_GRADIENT_STYLE, Styles.BOX_BORDER, Styles.H_SCREEN, Styles.W_SCREEN, 
        Styles.OVERFLOW_HIDDEN, Styles.FLEX);

  // Build bundle...
  bundle = new HtmlBundle()
    .setTitle("Page Title")
    .addHeaderContent(AdminView.renderNavBar())
    .addMainContent(mainContents)
    .addMainStyles(mainStyles);
    
  protected Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addFooterScripts("main");
    bundle.addMainStyles(MAIN_STYLES, isCentered ? CENTERED_STYLES : FULL_STYLES);
    bundle.addBodyStyles(BODY_STYLES);
    return htmlContent(bundle);
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, true);
  }

  public Content renderFull(HtmlBundle bundle) {
    return render(bundle, false);
  }

  /** MOVE TO ADMIN VIEW */
  private ContainerTag renderNavBar() {
    String questionLink = controllers.admin.routes.QuestionController.index().url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String versionLink = routes.AdminVersionController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();

    ContainerTag headerIcon =
        div(span("C"), span("F").withClasses(Styles.FONT_THIN))
            .withClasses(BaseStyles.ADMIN_NAV_BAR);
    ContainerTag headerTitle =
        div()
            .withClasses(
                Styles.FONT_NORMAL, Styles.INLINE, Styles.PL_10, Styles.PY_0, Styles.TEXT_XL)
            .with(span("Civi"), span("Form").withClasses(Styles.FONT_THIN));

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

  /** MOVE TO ADMIN VIEW */
  public Tag headerLink(String text, String href, String... styles) {
    return a(text)
        .withHref(href)
        .withClasses(
            Styles.PX_3,
            Styles.OPACITY_75,
            StyleUtils.hover(Styles.OPACITY_100),
            StyleUtils.joinStyles(styles));
  }
}
