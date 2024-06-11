package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;
import static views.BaseHtmlView.asRedirectElement;
import static views.ViewUtils.makeSvgTextButton;

import auth.CiviFormProfile;
import controllers.AssetsFinder;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.NavTag;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.JsBundle;
import views.ViewUtils;
import views.components.Icons;
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
    API_KEYS,
    SETTINGS,
    API_DOCS,
    EXPORT,
    IMPORT,
  }

  private final NavPage activeNavPage;
  private final TranslationLocales translationLocales;

  private AdminType primaryAdminType = AdminType.CIVI_FORM_ADMIN;

  AdminLayout(
      ViewUtils viewUtils,
      NavPage activeNavPage,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      AssetsFinder assetsFinder) {
    super(viewUtils, settingsManifest, deploymentType, assetsFinder);
    this.activeNavPage = activeNavPage;
    this.translationLocales = checkNotNull(translationLocales);
  }

  /**
   * Sets this layout's admin type based on the CiviFormProfile, used to determine which navigation
   * to include.
   */
  public AdminLayout setAdminType(CiviFormProfile profile) {
    primaryAdminType =
        profile.isOnlyProgramAdmin() ? AdminType.PROGRAM_ADMIN : AdminType.CIVI_FORM_ADMIN;
    return this;
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, /* isCentered= */ true);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    return render(bundle, /* isCentered= */ false);
  }

  private Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addMainStyles(
        AdminStyles.MAIN, isCentered ? AdminStyles.MAIN_CENTERED : AdminStyles.MAIN_FULL);
    bundle.addBodyStyles(AdminStyles.BODY);

    return super.render(bundle);
  }

  @Override
  protected String getTitleSuffix() {
    return "CiviForm admin console";
  }

  @Override
  public HtmlBundle getBundle(HtmlBundle bundle) {
    return super.getBundle(bundle)
        .addHeaderContent(renderNavBar(bundle.getRequest()))
        .setJsBundle(JsBundle.ADMIN);
  }

  /**
   * Creates a button that will redirect to the translations management page. Returns an empty
   * optional if there are no locales to translate to.
   */
  public Optional<ButtonTag> createManageTranslationsButton(
      String programAdminName, Optional<String> buttonId, String buttonStyles) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String linkDestination =
        routes.AdminProgramTranslationsController.redirectToFirstLocale(programAdminName).url();
    ButtonTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE).withClass(buttonStyles);
    buttonId.ifPresent(button::withId);
    return Optional.of(asRedirectElement(button, linkDestination));
  }

  private NavTag renderNavBar(Http.RequestHeader request) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();

    DivTag headerIcon =
        div(
            a().withHref(controllers.routes.HomeController.index().url())
                .with(
                    div(span("C"), span("F").withClasses("font-thin"))
                        .withClasses(AdminStyles.ADMIN_NAV_BAR_LOGO)));
    DivTag headerTitle =
        div()
            .withClasses("font-normal", "text-xl", "inline", "pl-10", "py-0", "mr-4")
            .with(span("Civi"), span("Form").withClasses("font-thin"));

    NavTag navBar = nav().with(getGovBanner(Optional.empty())).withClasses(AdminStyles.NAV_STYLES);

    DivTag adminHeader =
        div().with(headerIcon, headerTitle).withClasses(AdminStyles.INNER_NAV_STYLES);

    String questionLink = controllers.admin.routes.AdminQuestionController.index().url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String programAdminProgramsLink = controllers.admin.routes.ProgramAdminController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String apiKeysLink = controllers.admin.routes.AdminApiKeysController.index().url();
    String apiDocsLink = controllers.api.routes.ApiDocsController.index().url();
    String reportingLink = controllers.admin.routes.AdminReportingController.index().url();
    String exportLink = controllers.admin.routes.AdminExportController.index().url();
    String importLink = controllers.admin.routes.AdminImportController.index().url();
    String settingsLink = controllers.admin.routes.AdminSettingsController.index().url();

    String activeNavStyle =
        StyleUtils.joinStyles(
            BaseStyles.TEXT_CIVIFORM_BLUE,
            "font-medium",
            "border-b-2",
            BaseStyles.BORDER_CIVIFORM_BLUE);

    DomContent reportingHeaderLink =
        headerLink(
            "Reporting",
            reportingLink,
            NavPage.REPORTING.equals(activeNavPage) ? activeNavStyle : "");

    ATag programsHeaderLink =
        headerLink(
            "Programs", programLink, NavPage.PROGRAMS.equals(activeNavPage) ? activeNavStyle : "");

    ATag programAdminProgramsHeaderLink =
        headerLink(
            "Programs",
            programAdminProgramsLink,
            NavPage.PROGRAMS.equals(activeNavPage) ? activeNavStyle : "");

    ATag questionsHeaderLink =
        headerLink(
            "Questions",
            questionLink,
            NavPage.QUESTIONS.equals(activeNavPage) ? activeNavStyle : "");

    ATag intermediariesHeaderLink =
        headerLink(
            "Intermediaries",
            intermediaryLink,
            NavPage.INTERMEDIARIES.equals(activeNavPage) ? activeNavStyle : "");

    ATag apiKeysHeaderLink =
        headerLink(
            "API keys", apiKeysLink, NavPage.API_KEYS.equals(activeNavPage) ? activeNavStyle : "");

    ATag apiDocsHeaderLink =
        headerLink(
            "API docs", apiDocsLink, NavPage.API_DOCS.equals(activeNavPage) ? activeNavStyle : "");

    ATag exportHeaderLink =
        headerLink(
            "Export", exportLink, NavPage.EXPORT.equals(activeNavPage) ? activeNavStyle : "");
    ATag importHeaderLink =
        headerLink(
            "Import", importLink, NavPage.IMPORT.equals(activeNavPage) ? activeNavStyle : "");

    switch (primaryAdminType) {
      case CIVI_FORM_ADMIN:
        {
          adminHeader
              .with(programsHeaderLink)
              .with(questionsHeaderLink)
              .with(intermediariesHeaderLink)
              .with(reportingHeaderLink)
              .with(apiKeysHeaderLink)
              .condWith(
                  getSettingsManifest().getApiGeneratedDocsEnabled(request), apiDocsHeaderLink)
              .condWith(getSettingsManifest().getProgramMigrationEnabled(request), exportHeaderLink)
              .condWith(
                  getSettingsManifest().getProgramMigrationEnabled(request), importHeaderLink);
          break;
        }
      case PROGRAM_ADMIN:
        {
          adminHeader
              .with(programAdminProgramsHeaderLink)
              .with(reportingHeaderLink)
              .condWith(
                  getSettingsManifest().getApiGeneratedDocsEnabled(request), apiDocsHeaderLink);
          break;
        }
    }

    adminHeader.with(
        headerLink("Logout", logoutLink, "float-right").withId("logout-button"),
        primaryAdminType.equals(AdminType.CIVI_FORM_ADMIN)
            ? a(Icons.svg(Icons.COG)
                    .withClasses("h-6", "w-6", "opacity-75", StyleUtils.hover("opacity-100")))
                .withHref(settingsLink)
                .withClasses("float-right")
            : null);

    return navBar.with(adminHeader);
  }

  private ATag headerLink(String text, String href, String... styles) {
    return a(text)
        .withHref(href)
        .withClasses(
            "px-3", "opacity-75", StyleUtils.hover("opacity-100"), StyleUtils.joinStyles(styles));
  }
}
