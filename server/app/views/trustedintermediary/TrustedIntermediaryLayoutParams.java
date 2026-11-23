package views.trustedintermediary;

import auth.FakeAdminClient;
import com.google.common.collect.ImmutableList;
import controllers.routes;
import java.util.Optional;
import lombok.Builder;

/**
 * This record contains values used by the {@code
 * trustedintermediary/TrustedIntermediaryLayout.html} file and its partials. They are the pieces of
 * the legacy j2html {@link ApplicantLayout} skeleton that trusted intermediary pages rendered: the
 * banners, the navigation bar, the footer and the layout-owned modals.
 *
 * @param language Language code of the page, set on the {@code <html>} element
 * @param faviconUrl Link to the deployment's favicon, empty when unset
 * @param showNotProductionBanner Show the not-production banner in place of the official government
 *     website banner
 * @param productionUrl Link to the production site shown in the not-production banner, empty when
 *     unset
 * @param civicEntityFullName Full name of the civic entity, used in the logo alt text and the
 *     not-production banner link
 * @param civicEntityShortName Short name of the civic entity, shown next to the logo
 * @param hideCivicEntityName Hide the civic entity short name next to the logo
 * @param logoUrl Link to the logo in the navigation bar
 * @param govIconUrl Link to the .gov icon in the official government website banner
 * @param httpsIconUrl Link to the https icon in the official government website banner
 * @param isTrustedIntermediary The current user is a trusted intermediary
 * @param showDevTools Show the DevTools link and its modal
 * @param isDevOrStaging Add the toast warning against entering real data
 * @param updateLanguageUrl Target of the language switcher form
 * @param languageRedirectUrl Page the language switcher returns to
 * @param languageOptions Options of the language switcher
 * @param accountIdentifier Identifier of the logged in user shown in the navigation bar
 * @param supportEmail Technical support email address shown in the footer
 * @param showSessionTimeoutModals Add the session timeout warning modals
 */
@Builder
public record TrustedIntermediaryLayoutParams(
    String language,
    String faviconUrl,
    boolean showNotProductionBanner,
    String productionUrl,
    String civicEntityFullName,
    String civicEntityShortName,
    boolean hideCivicEntityName,
    String logoUrl,
    String govIconUrl,
    String httpsIconUrl,
    boolean isTrustedIntermediary,
    boolean showDevTools,
    boolean isDevOrStaging,
    String updateLanguageUrl,
    String languageRedirectUrl,
    ImmutableList<LanguageOption> languageOptions,
    String accountIdentifier,
    String supportEmail,
    boolean showSessionTimeoutModals) {

  /**
   * One option of the language switcher.
   *
   * @param code Language code, used as both the option value and its {@code lang}
   * @param label Name of the language, localized to that language
   * @param selected This is the language the page is rendered in
   */
  public record LanguageOption(String code, String label, boolean selected) {}

  public String homeUrl() {
    return routes.HomeController.index().url();
  }

  public String tiDashboardUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            /* nameQuery= */ Optional.empty(),
            /* dayQuery= */ Optional.empty(),
            /* monthQuery= */ Optional.empty(),
            /* yearQuery= */ Optional.empty(),
            /* page= */ Optional.of(1))
        .url();
  }

  public String logoutUrl() {
    return org.pac4j.play.routes.LogoutController.logout().url();
  }

  public String extendSessionUrl() {
    return routes.SessionController.extendSession().url();
  }

  public String fakeCiviformAdminUrl() {
    return fakeAdminUrl(FakeAdminClient.GLOBAL_ADMIN);
  }

  public String fakeProgramAdminUrl() {
    return fakeAdminUrl(FakeAdminClient.PROGRAM_ADMIN);
  }

  public String fakeDualAdminUrl() {
    return fakeAdminUrl(FakeAdminClient.DUAL_ADMIN);
  }

  public String fakeTrustedIntermediaryUrl() {
    return fakeAdminUrl(FakeAdminClient.TRUSTED_INTERMEDIARY);
  }

  public String additionalToolsUrl() {
    return controllers.dev.routes.DevToolsController.index().url();
  }

  private static String fakeAdminUrl(String adminType) {
    return routes.CallbackController.fakeAdmin(FakeAdminClient.CLIENT_NAME, adminType).url();
  }
}
