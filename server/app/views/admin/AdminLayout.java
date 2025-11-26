package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.header;
import static j2html.TagCreator.li;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static views.BaseHtmlView.asRedirectElement;
import static views.ViewUtils.makeSvgTextButton;

import auth.CiviFormProfile;
import controllers.AssetsFinder;
import controllers.admin.routes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.HeaderTag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.UlTag;
import java.util.Optional;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeploymentType;
import services.TranslationLocales;
import services.ViteService;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.ViewUtils;
import views.components.Icons;
import views.style.AdminStyles;

/** Contains methods rendering common compoments used across admin pages. */
public final class AdminLayout extends BaseHtmlLayout {

  private enum AdminType {
    CIVIFORM_ADMIN,
    PROGRAM_ADMIN
  }

  public enum NavPage {
    /** Used when no page is set */
    NULL_PAGE,
    PROGRAMS,
    QUESTIONS,
    INTERMEDIARIES,
    REPORTING,
    API_KEYS,
    SETTINGS,
    API_DOCS,
    API_BRIDGE_DISCOVERY
  }

  private final NavPage activeNavPage;
  private final TranslationLocales translationLocales;
  private final MessagesApi messagesApi;

  private AdminType primaryAdminType = AdminType.CIVIFORM_ADMIN;

  AdminLayout(
      ViewUtils viewUtils,
      NavPage activeNavPage,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      AssetsFinder assetsFinder,
      ViteService viteService,
      MessagesApi messagesApi) {
    super(viewUtils, settingsManifest, deploymentType, assetsFinder, viteService);
    this.activeNavPage = activeNavPage;
    this.translationLocales = checkNotNull(translationLocales);
    this.messagesApi = checkNotNull(messagesApi);
  }

  /**
   * Sets this layout's admin type based on the CiviFormProfile, used to determine which navigation
   * to include.
   */
  public AdminLayout setAdminType(CiviFormProfile profile) {
    primaryAdminType =
        profile.isOnlyProgramAdmin() ? AdminType.PROGRAM_ADMIN : AdminType.CIVIFORM_ADMIN;
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
    addSessionTimeoutModals(bundle, messagesApi.preferred(bundle.getRequest()));

    return super.render(bundle);
  }

  @Override
  protected String getTitleSuffix() {
    return "CiviForm admin console";
  }

  @Override
  public HtmlBundle getBundle(HtmlBundle bundle) {
    return super.getBundle(bundle).addHeaderContent(renderNavBar(bundle.getRequest()));
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

  /**
   * Creates a button that will take a user back to the previous page.
   *
   * @param goBackUrl the URL to return to
   * @param goBackText the text to display on the button
   * @return a DivTag containing the button
   */
  public DivTag createGoBackButton(String goBackUrl, String goBackText) {
    return div(a(Icons.svg(Icons.ARROW_LEFT).withClasses("w-5", "h-5", "mr-2"))
            .with(text(goBackText))
            .withHref(goBackUrl)
            .withClasses("text-blue-600", "hover:text-blue-500", "inline-flex", "items-center"))
        .withClasses("grid-row");
  }

  private NavTag renderNavBar(Http.RequestHeader request) {
    DivTag headerIcon =
        div(
            a().withHref(controllers.routes.HomeController.index().url())
                .with(
                    div(span("C"), span("F").withClasses("text-light"))
                        .withClasses(
                            "bg-base-dark",
                            "width-4",
                            "margin-05",
                            "font-sans-lg",
                            "radius-md",
                            "grid-row",
                            "flex-align-center",
                            "flex-justify-center",
                            "text-white"))
                .withClasses("text-no-underline", "text-white", "text-base-lightest"));

    DivTag headerTitle =
        div()
            .withClasses("font-sans-lg", "padding-y-0", "margin-right-2")
            .with(span("Civi"), span("Form").withClasses("text-light"));

    NavTag navBar =
        nav()
            .condWith(
                !settingsManifest.getShowNotProductionBannerEnabled(request),
                getGovBanner(Optional.empty()))
            .withClasses("position-fixed", "top-0", "width-full", "z-10");

    HeaderTag headerAccordion =
        header()
            .withClasses("usa-header", "usa-header--basic", "display-inline-block", "width-full")
            .with(
                nav().attr("aria-label", "Primary navigation").with(createAdminHeaderUl(request)));

    DivTag adminHeader =
        div()
            .with(headerIcon, headerTitle, headerAccordion)
            .withClasses(
                "shadow-1",
                "bg-white",
                "text-base-darker",
                "padding-x-2",
                "grid-row",
                "flex-no-wrap",
                "flex-align-center");

    return navBar.with(adminHeader);
  }

  private UlTag createAdminHeaderUl(Http.RequestHeader request) {

    UlTag adminHeaderUl =
        ul().withClasses(
                "usa-nav__primary",
                "usa-accordion",
                "grid-row",
                "flex-align-center",
                "padding-y-105");

    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    String questionLink =
        controllers.admin.routes.AdminQuestionController.index(Optional.empty()).url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String programAdminProgramsLink = controllers.admin.routes.ProgramAdminController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String apiKeysLink = controllers.admin.routes.AdminApiKeysController.index().url();
    String apiDocsLink = controllers.docs.routes.ApiDocsController.index().url();
    String apiBridgeDiscoveryLink =
        controllers.admin.apibridge.routes.DiscoveryController.discovery().url();
    String reportingLink = controllers.admin.routes.AdminReportingController.index().url();
    String settingsLink = controllers.admin.routes.AdminSettingsController.index().url();

    LiTag programsNavItem =
        createTopNavItem(
            "Programs", programLink, NavPage.PROGRAMS.equals(activeNavPage), Optional.empty());

    LiTag programAdminProgramsHeaderLink =
        createTopNavItem(
            "Programs",
            programAdminProgramsLink,
            NavPage.PROGRAMS.equals(activeNavPage),
            Optional.empty());

    LiTag questionsNavItem =
        createTopNavItem(
            "Questions", questionLink, NavPage.QUESTIONS.equals(activeNavPage), Optional.empty());

    LiTag intermediariesNavItem =
        createTopNavItem(
            "Intermediaries",
            intermediaryLink,
            NavPage.INTERMEDIARIES.equals(activeNavPage),
            Optional.empty());

    LiTag reportingNavItem =
        createTopNavItem(
            "Reporting", reportingLink, NavPage.REPORTING.equals(activeNavPage), Optional.empty());

    LiTag settingsNavItem =
        li(
            a(Icons.svg(Icons.COG)
                    .withClasses("height-3", "width-3", "opacity-75", "hover:opacity-100"))
                .withHref(settingsLink)
                .withClasses("cf-admin-heading-nav-item-settings-icon"));

    LiTag logoutNavItem =
        createTopNavItem("Logout", logoutLink, false, Optional.of("logout-button"));

    String apiNavItemDropdownId = "admin-api-nav-item";

    LiTag apiNavItemDropdown =
        createTopNavItemWithDropdown(
                "API",
                apiNavItemDropdownId,
                NavPage.API_KEYS.equals(activeNavPage) || NavPage.API_DOCS.equals(activeNavPage))
            .with(
                ul().withId(apiNavItemDropdownId)
                    .withClasses("usa-nav__submenu")
                    .with(
                        createDropdownSubItem(
                            "API Keys", apiKeysLink, NavPage.API_KEYS.equals(activeNavPage)))
                    .condWith(
                        getSettingsManifest().getApiGeneratedDocsEnabled(request),
                        createDropdownSubItem(
                            "Documentation", apiDocsLink, NavPage.API_DOCS.equals(activeNavPage)))
                    .condWith(
                        getSettingsManifest().getApiBridgeEnabled(request),
                        createDropdownSubItem(
                            "Bridge Discovery",
                            apiBridgeDiscoveryLink,
                            NavPage.API_BRIDGE_DISCOVERY.equals(activeNavPage))));

    LiTag programAdminApiNavItemDropdown =
        createTopNavItemWithDropdown(
                "API", apiNavItemDropdownId, NavPage.API_DOCS.equals(activeNavPage))
            .with(
                ul().withId(apiNavItemDropdownId)
                    .withClasses("usa-nav__submenu")
                    .with(
                        createDropdownSubItem(
                            "Documentation", apiDocsLink, NavPage.API_DOCS.equals(activeNavPage))));

    switch (primaryAdminType) {
      case CIVIFORM_ADMIN ->
          adminHeaderUl.with(
              programsNavItem,
              questionsNavItem,
              intermediariesNavItem,
              reportingNavItem,
              apiNavItemDropdown,
              settingsNavItem.withClasses("usa-nav__primary-item", "margin-left-auto"),
              logoutNavItem);
      case PROGRAM_ADMIN ->
          adminHeaderUl
              .with(programAdminProgramsHeaderLink, reportingNavItem)
              .condWith(
                  getSettingsManifest().getApiGeneratedDocsEnabled(request),
                  programAdminApiNavItemDropdown)
              .with(logoutNavItem.withClasses("usa-nav__primary-item", "margin-left-auto"));
    }

    return adminHeaderUl;
  }

  private LiTag createTopNavItem(String text, String href, Boolean active, Optional<String> id) {
    ATag aTag =
        a(span(text).withClasses("text-normal", "font-sans-md", "line-height-sans-6"))
            .withHref(href)
            .withClasses("usa-nav__link", "cf-admin-heading-nav-item", active ? "usa-current" : "");

    id.ifPresent(x -> aTag.withId(x));

    return li(aTag).withClasses("usa-nav__primary-item");
  }

  private LiTag createTopNavItemWithDropdown(String text, String ariaControlsId, Boolean active) {
    return li(button(span(text).withClasses("text-normal", "font-sans-md", "line-height-sans-6"))
            .withType("button")
            .attr("aria-expanded", false)
            .attr("aria-controls", ariaControlsId)
            .withClasses(
                "usa-accordion__button",
                "usa-nav__link",
                "cf-admin-heading-nav-item",
                active ? "usa-current" : ""))
        .withClasses("usa-nav__primary-item");
  }

  private LiTag createDropdownSubItem(String text, String href, Boolean active) {
    return li(a(span(text)).withHref(href))
        .withClasses("usa-nav__submenu-item", active ? "text-bold" : "");
  }
}
