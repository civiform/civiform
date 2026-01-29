package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.header;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.text;
import static views.BaseHtmlView.asRedirectElement;
import static views.ViewUtils.makeSvgTextButton;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.MessagesApi;
import play.twirl.api.Content;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.JsBundle;
import views.ViewUtils;
import views.admin.shared.AdminCommonHeader;
import views.components.Icons;
import views.style.AdminStyles;

/** Contains methods rendering common compoments used across admin pages. */
public final class AdminLayout extends BaseHtmlLayout {
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
  private final ProfileUtils profileUtils;

  AdminLayout(
      ViewUtils viewUtils,
      NavPage activeNavPage,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      BundledAssetsFinder bundledAssetsFinder,
      MessagesApi messagesApi,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      TemplateEngine templateEngine,
      ProfileUtils profileUtils) {
    super(
        viewUtils,
        settingsManifest,
        deploymentType,
        bundledAssetsFinder,
        playThymeleafContextFactory,
        templateEngine);
    this.activeNavPage = activeNavPage;
    this.translationLocales = checkNotNull(translationLocales);
    this.messagesApi = checkNotNull(messagesApi);
    this.profileUtils = checkNotNull(profileUtils);
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
    ThymeleafModule.PlayThymeleafContext context =
        new ThymeleafModule.PlayThymeleafContext(Locale.getDefault());
    CiviFormProfile profile = profileUtils.currentUserProfile(bundle.getRequest());

    context.setVariable(
        "adminCommonHeader",
        AdminCommonHeader.builder()
            .activeNavPage(activeNavPage)
            .isOnlyProgramAdmin(profile.isOnlyProgramAdmin())
            .isApiBridgeEnabled(settingsManifest.getApiBridgeEnabled(bundle.getRequest()))
            .build());

    var header = templateEngine.process("admin/shared/AdminCommonHeader.html", context);

    return super.getBundle(bundle).addHeaderContent(rawHtml(header)).setJsBundle(JsBundle.ADMIN);
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
}
