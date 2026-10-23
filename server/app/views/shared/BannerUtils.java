package views.shared;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import services.settings.SettingsManifest;

/** Utility methods for configuring and rendering site banners (not-production and demo banners). */
public final class BannerUtils {
  private static final Logger logger = LoggerFactory.getLogger(BannerUtils.class);

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
      logger.warn(
          "The not-production banner is enabled, but configured with the environment value"
              + " `SHOW_NOT_PRODUCTION_BANNER_ENABLED`. Please migrate to using `BANNER_TYPE`. See"
              + " the latest server environment variable documentation for details."
              + " https://docs.civiform.us/it-manual/sre-playbook/upgrading-to-a-new-release/server-environment-variables");
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
   * Returns the demo banner days remaining count if an expiration date is set and valid.
   *
   * @param settingsManifest the settings manifest to get the expiration date from
   * @param currentDate the current date to calculate remaining days against
   * @return remaining days count, or empty if missing, invalid, or negative
   */
  public static Optional<Long> getDemoBannerDaysRemainingCount(
      SettingsManifest settingsManifest, LocalDate currentDate) {
    Optional<String> maybeExpirationDate = settingsManifest.getDemoBannerExpirationDate();
    if (maybeExpirationDate.isEmpty() || maybeExpirationDate.get().isBlank()) {
      return Optional.empty();
    }
    try {
      LocalDate expirationDate =
          LocalDate.parse(maybeExpirationDate.get().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      long daysRemaining = ChronoUnit.DAYS.between(currentDate, expirationDate);
      if (daysRemaining < 0) {
        return Optional.empty();
      }
      return Optional.of(daysRemaining);
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  /**
   * Returns the demo banner days remaining count against the current system date if an expiration
   * date is set and valid.
   */
  public static Optional<Long> getDemoBannerDaysRemainingCount(SettingsManifest settingsManifest) {
    return getDemoBannerDaysRemainingCount(settingsManifest, LocalDate.now(ZoneId.systemDefault()));
  }

  /**
   * Returns the CSS color classes for the demo banner expiration tag.
   *
   * @param daysRemaining the count of remaining days
   * @return "bg-green" if > 5 days remain, "bg-yellow text-ink" if 5 or less days remain
   */
  public static String getDemoBannerTagColorClass(long daysRemaining) {
    return daysRemaining > 5 ? "bg-green" : "bg-yellow text-ink";
  }
}
