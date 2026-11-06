package views.shared;

import auth.CiviFormProfile;
import auth.FakeAdminClient;
import controllers.routes;
import java.time.Clock;
import java.util.Optional;
import lombok.Builder;
import play.mvc.Http;
import services.settings.SettingsManifest;
import views.components.Icons;

/**
 * View model containing values used by {@code shared/DemoBannerFragment.html} and the Applicant
 * DevTools modal.
 */
@Builder
public record DemoBanner(
    boolean enabled,
    boolean showDebugTools,
    String civicEntityShortName,
    Optional<Long> daysRemainingCount,
    boolean isTrustedIntermediary,
    boolean isCiviFormAdmin,
    boolean isOnlyProgramAdmin,
    boolean isAdminOrTi) {

  /** Creates a {@link DemoBanner} populated from settings, deployment type, and user profile. */
  public static DemoBanner create(
      SettingsManifest settingsManifest,
      Http.RequestHeader request,
      Optional<CiviFormProfile> profile,
      boolean isDevOrStaging,
      Clock clock) {
    boolean enabled = BannerUtils.isDemoBannerEnabled(settingsManifest, request);
    boolean showDebugTools =
        isDevOrStaging && !settingsManifest.getStagingDisableDemoModeLogins(request);

    return DemoBanner.builder()
        .enabled(enabled)
        .showDebugTools(showDebugTools)
        .civicEntityShortName(
            enabled ? settingsManifest.getWhitelabelCivicEntityShortName(request).orElse("") : "")
        .daysRemainingCount(
            enabled
                ? BannerUtils.getDemoBannerDaysRemainingCount(settingsManifest, clock)
                : Optional.empty())
        .isTrustedIntermediary(profile.map(CiviFormProfile::isTrustedIntermediary).orElse(false))
        .isCiviFormAdmin(profile.map(CiviFormProfile::isCiviFormAdmin).orElse(false))
        .isOnlyProgramAdmin(profile.map(CiviFormProfile::isOnlyProgramAdmin).orElse(false))
        .isAdminOrTi(
            profile
                .map(p -> p.isCiviFormAdmin() || p.isProgramAdmin() || p.isTrustedIntermediary())
                .orElse(false))
        .build();
  }

  public boolean isResident() {
    return !isAdminOrTi;
  }

  public String logoutUrl() {
    return org.pac4j.play.routes.LogoutController.logout().url();
  }

  public String fakeCiviformAdminUrl() {
    return routes.CallbackController.fakeAdmin(
            FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
        .url();
  }

  public String fakeProgramAdminUrl() {
    return routes.CallbackController.fakeAdmin(
            FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
        .url();
  }

  public String fakeDualAdminUrl() {
    return routes.CallbackController.fakeAdmin(
            FakeAdminClient.CLIENT_NAME, FakeAdminClient.DUAL_ADMIN)
        .url();
  }

  public String fakeTrustedIntermediaryUrl() {
    return routes.CallbackController.fakeAdmin(
            FakeAdminClient.CLIENT_NAME, FakeAdminClient.TRUSTED_INTERMEDIARY)
        .url();
  }

  public String additionalToolsUrl() {
    return controllers.dev.routes.DevToolsController.index().url();
  }

  public Icons cogIcon() {
    return Icons.COG;
  }
}
