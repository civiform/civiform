package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import com.typesafe.config.Config;
import controllers.admin.routes;
import featureflags.FeatureFlags;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.NavTag;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;

/** Contains methods rendering common compoments used across admin pages. */
public final class AdminLayout extends BaseHtmlLayout {

  public enum AdminType {
    CIVI_FORM_ADMIN,
    PROGRAM_ADMIN
  }

  public enum NavPage {
    PROGRAMS,
    QUESTIONS,
    INTERMEDIARIES,
    REPORTING,
    API_KEYS
  }

  private final NavPage activeNavPage;

  private static final String[] FOOTER_SCRIPTS = {"preview", "questionBank", "admin_validation"};

  private AdminType primaryAdminType = AdminType.CIVI_FORM_ADMIN;

  AdminLayout(
      ViewUtils viewUtils, Config configuration, FeatureFlags featureFlags, NavPage activeNavPage) {
    super(viewUtils, configuration, featureFlags);
    this.activeNavPage = activeNavPage;
  }

  /**
   * Sets this layout's admin type to PROGRAM_ADMIN, used to determine which navigation to include.
   */
  public AdminLayout setOnlyProgramAdminType() {
    primaryAdminType = AdminType.PROGRAM_ADMIN;
    return this;
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, /* isCentered = */ true);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    return render(bundle, /* isCentered = */ false);
  }

  private Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addMainStyles(
        AdminStyles.MAIN, isCentered ? AdminStyles.MAIN_CENTERED : AdminStyles.MAIN_FULL);
    bundle.addBodyStyles(AdminStyles.BODY);

    for (String source : FOOTER_SCRIPTS) {
      bundle.addFooterScripts(viewUtils.makeLocalJsTag(source));
    }

    return super.render(bundle);
  }

  @Override
  protected String getTitleSuffix() {
    return "CiviForm Admin Console";
  }

  @Override
  public HtmlBundle getBundle(HtmlBundle bundle) {
    return super.getBundle(bundle).addHeaderContent(renderNavBar());
  }

  private NavTag renderNavBar() {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();

    DivTag headerIcon =
        div(span("C"), span("F").withClasses("font-thin")).withClasses(AdminStyles.ADMIN_NAV_BAR);
    DivTag headerTitle =
        div()
            .withClasses("font-normal", "text-xl", "inline", "pl-10", "py-0", "mr-4")
            .with(span("Civi"), span("Form").withClasses("font-thin"));

    NavTag adminHeader = nav().with(headerIcon, headerTitle).withClasses(AdminStyles.NAV_STYLES);

    // Don't include nav links for program admin.
    if (primaryAdminType.equals(AdminType.PROGRAM_ADMIN)) {
      return adminHeader.with(headerLink("Logout", logoutLink, "float-right"));
    }

    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String questionLink = controllers.admin.routes.AdminQuestionController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String apiKeysLink = controllers.admin.routes.AdminApiKeysController.index().url();
    String reportingLink = controllers.admin.routes.AdminReportingController.index().url();

    String activeNavStyle =
        StyleUtils.joinStyles(
            BaseStyles.TEXT_SEATTLE_BLUE,
            "font-medium",
            "border-b-2",
            BaseStyles.BORDER_SEATTLE_BLUE);

    DomContent reportingHeaderLink =
        headerLink(
            "Reporting",
            reportingLink,
            NavPage.REPORTING.equals(activeNavPage) ? activeNavStyle : "");

    return adminHeader
        .with(
            headerLink(
                "Programs",
                programLink,
                NavPage.PROGRAMS.equals(activeNavPage) ? activeNavStyle : ""))
        .with(
            headerLink(
                "Questions",
                questionLink,
                NavPage.QUESTIONS.equals(activeNavPage) ? activeNavStyle : ""))
        .with(
            headerLink(
                "Intermediaries",
                intermediaryLink,
                NavPage.INTERMEDIARIES.equals(activeNavPage) ? activeNavStyle : ""))
        .with(
            headerLink(
                "API keys",
                apiKeysLink,
                NavPage.API_KEYS.equals(activeNavPage) ? activeNavStyle : ""))
        .with(getFeatureFlags().isAdminReportingUiEnabled() ? reportingHeaderLink : null)
        .with(headerLink("Logout", logoutLink, "float-right"));
  }

  private ATag headerLink(String text, String href, String... styles) {
    return a(text)
        .withHref(href)
        .withClasses(
            "px-3", "opacity-75", StyleUtils.hover("opacity-100"), StyleUtils.joinStyles(styles));
  }
}
