package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class AdminLayout extends BaseHtmlLayout {

  @Inject
  public AdminLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  @Override
  public HtmlBundle getBundle() {
    return getBundle(new HtmlBundle());
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, /* isCentered = */ true);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    return render(bundle, /* isCentered = */ false);
  }

  protected Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addMainStyles(AdminStyles.MAIN, isCentered ? AdminStyles.CENTERED : AdminStyles.FULL);
    bundle.addBodyStyles(AdminStyles.BODY);
    String currentTitle = bundle.getTitle();
    if (currentTitle != null && !currentTitle.isEmpty()) {
      bundle.setTitle(currentTitle + " - CiviForm Admin Console");
    }
    return super.render(bundle);
  }

  @Override
  public HtmlBundle getBundle(HtmlBundle bundle) {
    return super.getBundle(bundle).addHeaderContent(renderNavBar());
  }

  private ContainerTag renderNavBar() {
    String questionLink = controllers.admin.routes.AdminQuestionController.index().url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String versionLink = routes.AdminVersionController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();

    ContainerTag headerIcon =
        div(span("C"), span("F").withClasses(Styles.FONT_THIN))
            .withClasses(AdminStyles.ADMIN_NAV_BAR);
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
            .withClasses(AdminStyles.NAV_STYLES);
    return adminHeader;
  }

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
