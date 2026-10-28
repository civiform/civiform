package views.shared;

import auth.CiviFormProfile;
import auth.FakeAdminClient;
import controllers.routes;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import modules.ThymeleafModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import services.settings.SettingsManifest;
import views.components.Icons;

/** Utility methods for configuring and rendering site banners (not-production and demo banners). */
public final class BannerUtils {
  private static final Logger logger = LoggerFactory.getLogger(BannerUtils.class);
  private static final AtomicBoolean hasLoggedDeprecationWarning = new AtomicBoolean(false);

  private BannerUtils() {}

  /**
   * Returns true if the non-production banner should be displayed, checking {@code BANNER_TYPE}
   * first and falling back to the deprecated {@code SHOW_NOT_PRODUCTION_BANNER_ENABLED} flag when
   * {@code BANNER_TYPE} is unset or {@code "none"}.
   */
  public static boolean isNotProductionBannerEnabled(
      SettingsManifest settingsManifest, Http.RequestHeader request) {
    Optional<String> bannerType = settingsManifest.getBannerType(request);
    if (bannerType.isPresent() && !bannerType.get().isBlank() && !bannerType.get().equals("none")) {
      return bannerType.get().equals("not_production");
    }
    if (settingsManifest.getShowNotProductionBannerEnabled(request)) {
      if (hasLoggedDeprecationWarning.compareAndSet(false, true)) {
        logger.warn(
            "The not-production banner is enabled, but configured with the environment value"
                + " `SHOW_NOT_PRODUCTION_BANNER_ENABLED`. Please migrate to using `BANNER_TYPE`."
                + " See the latest server environment variable documentation for details."
                + " https://docs.civiform.us/it-manual/sre-playbook/upgrading-to-a-new-release/server-environment-variables");
      }
      return true;
    }
    return false;
  }

  /** Returns true if the demo banner should be displayed ({@code BANNER_TYPE == "demo"}). */
  public static boolean isDemoBannerEnabled(
      SettingsManifest settingsManifest, Http.RequestHeader request) {
    return settingsManifest.getBannerType(request).map(type -> type.equals("demo")).orElse(false);
  }

  /**
   * Returns the demo banner days remaining count if an expiration date is set, valid, and in the
   * future.
   *
   * @param settingsManifest the settings manifest to get the expiration date from
   * @param clock the clock used to determine today's date
   * @return remaining days count (> 0), or empty if missing, invalid, or not in the future
   */
  public static Optional<Long> getDemoBannerDaysRemainingCount(
      SettingsManifest settingsManifest, Clock clock) {
    Optional<String> maybeExpirationDate = settingsManifest.getDemoBannerExpirationDate();
    if (maybeExpirationDate.isEmpty() || maybeExpirationDate.get().isBlank()) {
      return Optional.empty();
    }
    try {
      LocalDate expirationDate =
          LocalDate.parse(maybeExpirationDate.get().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(clock), expirationDate);
      if (daysRemaining <= 0) {
        return Optional.empty();
      }
      return Optional.of(daysRemaining);
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  /**
   * Returns the demo banner days remaining count against the current system date if an expiration
   * date is set, valid, and in the future.
   */
  public static Optional<Long> getDemoBannerDaysRemainingCount(SettingsManifest settingsManifest) {
    return getDemoBannerDaysRemainingCount(settingsManifest, Clock.system(ZoneId.systemDefault()));
  }

  /**
   * Populates the shared Thymeleaf context variables for the demo banner and role login buttons on
   * Applicant and Admin Thymeleaf layouts.
   */
  public static void setUpBannerContext(
      ThymeleafModule.PlayThymeleafContext context,
      SettingsManifest settingsManifest,
      Http.Request request,
      Optional<CiviFormProfile> profile,
      boolean isDevOrStaging) {
    setUpBannerContext(
        context,
        settingsManifest,
        request,
        profile,
        isDevOrStaging,
        Clock.system(ZoneId.systemDefault()));
  }

  /**
   * Populates the shared Thymeleaf context variables for the demo banner and role login buttons on
   * Applicant and Admin Thymeleaf layouts.
   */
  public static void setUpBannerContext(
      ThymeleafModule.PlayThymeleafContext context,
      SettingsManifest settingsManifest,
      Http.Request request,
      Optional<CiviFormProfile> profile,
      boolean isDevOrStaging,
      Clock clock) {
    boolean isTi = profile.map(CiviFormProfile::isTrustedIntermediary).orElse(false);
    boolean isCiviFormAdmin = profile.map(CiviFormProfile::isCiviFormAdmin).orElse(false);
    boolean isOnlyProgramAdmin = profile.map(CiviFormProfile::isOnlyProgramAdmin).orElse(false);
    boolean isAdminOrTi =
        profile
            .map(p -> p.isCiviFormAdmin() || p.isProgramAdmin() || p.isTrustedIntermediary())
            .orElse(false);

    context.setVariable("isTrustedIntermediary", isTi);
    context.setVariable("isCiviFormAdmin", isCiviFormAdmin);
    context.setVariable("isOnlyProgramAdmin", isOnlyProgramAdmin);
    context.setVariable("isAdminOrTi", isAdminOrTi);
    context.setVariable("logoutLink", org.pac4j.play.routes.LogoutController.logout().url());
    context.setVariable("cogIcon", Icons.COG);

    boolean demoBannerEnabled = isDemoBannerEnabled(settingsManifest, request);
    context.setVariable("demoBannerEnabled", demoBannerEnabled);
    if (demoBannerEnabled) {
      String civicEntityShortName =
          settingsManifest.getWhitelabelCivicEntityShortName(request).orElse("");
      context.setVariable("civicEntityShortName", civicEntityShortName);
      context.setVariable(
          "demoBannerDaysRemainingCount",
          getDemoBannerDaysRemainingCount(settingsManifest, clock).orElse(null));
    }

    boolean showDebugTools =
        isDevOrStaging && !settingsManifest.getStagingDisableDemoModeLogins(request);
    context.setVariable("showDebugTools", showDebugTools);
    if (showDebugTools) {
      context.setVariable(
          "fakeCiviformAdminUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
              .url());
      context.setVariable(
          "fakeProgramAdminUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
              .url());
      context.setVariable(
          "fakeDualAdminUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.DUAL_ADMIN)
              .url());
      context.setVariable(
          "fakeTrustedIntermediaryUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.TRUSTED_INTERMEDIARY)
              .url());
      context.setVariable(
          "additionalToolsUrl", controllers.dev.routes.DevToolsController.index().url());
    }
  }
}
