package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Contains methods rendering common compoments used across admin pages. */
public class AdminLayout extends BaseHtmlLayout {

  public enum AdminType {
    CIVI_FORM_ADMIN,
    PROGRAM_ADMIN
  }

  private static final String[] FOOTER_SCRIPTS = {"preview", "questionBank"};

  private AdminType adminType = AdminType.CIVI_FORM_ADMIN;

  @Inject
  public AdminLayout(ViewUtils viewUtils, Config configuration) {
    super(viewUtils, configuration);
  }

  /**
   * Sets this layout's admin type to PROGRAM_ADMIN, used to determine which navigation to include.
   */
  public AdminLayout setProgramAdminType() {
    adminType = AdminType.PROGRAM_ADMIN;
    return this;
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, /* isCentered = */ true);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    return render(bundle, /* isCentered = */ false);
  }

  protected Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addMainStyles(
        AdminStyles.MAIN, isCentered ? AdminStyles.MAIN_CENTERED : AdminStyles.MAIN_FULL);
    bundle.addBodyStyles(AdminStyles.BODY);
    String currentTitle = bundle.getTitle();

    if (currentTitle != null && !currentTitle.isEmpty()) {
      bundle.setTitle(currentTitle + " - CiviForm Admin Console");
    }

    for (String source : FOOTER_SCRIPTS) {
      bundle.addFooterScripts(viewUtils.makeLocalJsTag(source));
    }

    return super.render(bundle);
  }

  @Override
  public HtmlBundle getBundle(HtmlBundle bundle) {
    return super.getBundle(bundle).addHeaderContent(renderNavBar());
  }

  private ContainerTag renderNavBar() {
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
        nav().with(headerIcon, headerTitle).withClasses(AdminStyles.NAV_STYLES);

    // Don't include nav links for program admin.
    if (adminType.equals(AdminType.PROGRAM_ADMIN)) {
      return adminHeader.with(headerLink("Logout", logoutLink, Styles.FLOAT_RIGHT));
    }

    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String questionLink = controllers.admin.routes.AdminQuestionController.index().url();
    String versionLink = routes.AdminVersionController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();

    return adminHeader
        .with(headerLink("Programs", programLink))
        .with(headerLink("Questions", questionLink))
        .with(headerLink("Versions", versionLink))
        .with(headerLink("Intermediaries", intermediaryLink))
        .with(headerLink("Logout", logoutLink, Styles.FLOAT_RIGHT));
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

  /** Renders a div with internal/admin program information. */
  public Tag renderProgramInfo(ProgramDefinition programDefinition) {
    ContainerTag programStatus =
        div("Draft").withId("program-status").withClasses(Styles.TEXT_XS, Styles.UPPERCASE);
    ContainerTag programTitle =
        div(programDefinition.adminName())
            .withId("program-title")
            .withClasses(Styles.TEXT_3XL, Styles.PB_3);
    ContainerTag programDescription =
        div(programDefinition.adminDescription()).withClasses(Styles.TEXT_SM);

    return div(programStatus, programTitle, programDescription)
        .withId("program-info")
        .withClasses(
            Styles.BG_GRAY_100,
            Styles.TEXT_GRAY_800,
            Styles.SHADOW_MD,
            Styles.P_8,
            Styles.PT_4,
            Styles._MX_2);
  }
}
