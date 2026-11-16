package views.trustedintermediary;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.LanguageUtils;
import java.util.Optional;
import modules.ThymeleafModule;
import play.i18n.Lang;
import play.mvc.Http;
import services.BundledAssetsFinder;
import views.BaseView;
import views.BaseViewModel;
import views.LayoutTemplate;
import views.shared.BannerUtils;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;
import views.trustedintermediary.TrustedIntermediaryLayoutParams.LanguageOption;

/**
 * {@link TrustedIntermediaryLayoutBaseView} is used to render the supplied Thymeleaf page template.
 * This view is tied to layout template {@link LayoutTemplate#TRUSTED_INTERMEDIARY_LAYOUT}, which
 * mirrors the legacy j2html {@link ApplicantLayout} page skeleton the trusted intermediary pages
 * render in, including the Tailwind stylesheet and the applicant JS bundle, so that 1:1 page
 * migrations keep their original styling and behavior.
 *
 * <p>This view is transitional. Use it only for trusted intermediary pages migrated 1:1 from
 * j2html.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class TrustedIntermediaryLayoutBaseView<TModel extends BaseViewModel>
    extends BaseView<TModel> {
  private final BundledAssetsFinder bundledAssetsFinder;
  private final LanguageUtils languageUtils;
  private final boolean isDevOrStaging;
  protected final ProfileUtils profileUtils;

  public TrustedIntermediaryLayoutBaseView(LayoutDeps layoutDeps, LanguageUtils languageUtils) {
    super(layoutDeps.baseViewDeps());
    this.bundledAssetsFinder = layoutDeps.bundledAssetsFinder();
    this.profileUtils = layoutDeps.profileUtils();
    this.isDevOrStaging = layoutDeps.deploymentType().isDevOrStaging();
    this.languageUtils = checkNotNull(languageUtils);
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);
    Lang preferredLanguage = languageUtils.getPreferredLanguage(request);

    // The language switcher posts to the trusted intermediary's own applicant, the id the legacy
    // views passed to ApplicantLayout.renderWithNav.
    long tiApplicantId = profile.getApplicant().join().id;

    context.setVariable(
        "tiLayout",
        TrustedIntermediaryLayoutParams.builder()
            .language(preferredLanguage.code())
            .faviconUrl(settingsManifest.getFaviconUrl().orElse(""))
            .showNotProductionBanner(
                BannerUtils.isNotProductionBannerEnabled(settingsManifest, request))
            .productionUrl(settingsManifest.getCivicEntityProductionUrl(request).orElse(""))
            .civicEntityFullName(
                settingsManifest.getWhitelabelCivicEntityFullName(request).orElse(""))
            .civicEntityShortName(
                settingsManifest.getWhitelabelCivicEntityShortName(request).orElse(""))
            .hideCivicEntityName(settingsManifest.getHideCivicEntityNameInHeader(request))
            .logoUrl(
                settingsManifest
                    .getCivicEntitySmallLogoUrl()
                    .orElseGet(() -> bundledAssetsFinder.path("Images/civiform-staging.png")))
            .govIconUrl(bundledAssetsFinder.path("Images/uswds/icon-dot-gov.svg"))
            .httpsIconUrl(bundledAssetsFinder.path("Images/uswds/icon-https.svg"))
            .isTrustedIntermediary(profile.isTrustedIntermediary())
            .showDevTools(
                isDevOrStaging && !settingsManifest.getStagingDisableDemoModeLogins(request))
            .isDevOrStaging(isDevOrStaging)
            .updateLanguageUrl(
                controllers.applicant.routes.ApplicantInformationController.setLangFromSwitcher(
                        tiApplicantId)
                    .url())
            // Trusted intermediary routes carry no program, so the legacy redirect logic always
            // came back to the current page.
            .languageRedirectUrl(request.uri())
            .languageOptions(buildLanguageOptions(preferredLanguage))
            .accountIdentifier(tiEmailForDisplay(profile))
            .supportEmail(settingsManifest.getSupportEmailAddress(request).orElse(""))
            .showSessionTimeoutModals(settingsManifest.getSessionTimeoutEnabled())
            .build());
  }

  /**
   * An option consists of the language localized to that language, for example 'Español' for es-US,
   * and the value is the ISO code.
   */
  private ImmutableList<LanguageOption> buildLanguageOptions(Lang preferredLanguage) {
    return languageUtils.getApplicantEnabledLanguages().stream()
        .map(
            lang -> {
              String code = lang.toLocale().toLanguageTag();

              return new LanguageOption(
                  code,
                  languageUtils.getDisplayString(lang.toLocale()),
                  code.equals(preferredLanguage.code()));
            })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * For TIs we use the account email rather than first and last name because TIs usually do not
   * have the latter data available, but will always have an email address because they are
   * authenticated.
   */
  private static String tiEmailForDisplay(CiviFormProfile profile) {
    // CommonProfile.getEmail() can return null, so we guard that with a generic
    // display string.
    String email =
        Optional.ofNullable(profile.getProfileData().getEmail()).orElse("Trusted Intermediary");

    // To ensure a consistent string with browser snapshots, we override the
    // display email.
    if (email.startsWith("fake-trusted-intermediary") && email.endsWith("@example.com")) {
      return "trusted-intermediary@example.com";
    }

    return email;
  }

  @Override
  protected final Optional<LayoutTemplate> layoutTemplate() {
    return Optional.of(LayoutTemplate.TRUSTED_INTERMEDIARY_LAYOUT);
  }

  /** Stylesheets in the same order the legacy j2html layout added them. */
  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.<String>builder()
        .add(bundledAssetsFinder.getUswdsStylesheet())
        .add(bundledAssetsFinder.getTailwindStylesheet())
        .add(bundledAssetsFinder.getMapLibreGLStylesheet())
        .build();
  }

  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteHeadScripts() {
    var builder = ImmutableList.<ScriptElementSettings>builder();

    if (bundledAssetsFinder.useBundlerDevServer()) {
      builder.add(ScriptElementSettings.builder().src(bundledAssetsFinder.viteClientUrl()).build());
    }

    return builder
        .add(
            ScriptElementSettings.builder()
                .src(bundledAssetsFinder.getUswdsJsInit())
                .type("text/javascript")
                .build())
        .build();
  }

  /**
   * The legacy j2html layout loaded both JS bundles from the footer at the end of the body. Trusted
   * intermediary pages use the applicant bundle, not the admin one.
   */
  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteBodyScripts() {
    return ImmutableList.<ScriptElementSettings>builder()
        .add(
            ScriptElementSettings.builder().src(bundledAssetsFinder.getApplicantJsBundle()).build())
        .add(ScriptElementSettings.builder().src(bundledAssetsFinder.getUswdsJsBundle()).build())
        .build();
  }
}
