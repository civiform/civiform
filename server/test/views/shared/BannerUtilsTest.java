package views.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;
import repository.ResetPostgres;
import services.settings.SettingsManifest;

public class BannerUtilsTest extends ResetPostgres {

  @Test
  public void isNotProductionBannerEnabled_returnsExpectedValues() {
    SettingsManifest notProdManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "not_production",
                    "show_not_production_banner_enabled", "false")));
    assertThat(BannerUtils.isNotProductionBannerEnabled(notProdManifest, fakeRequest())).isTrue();

    SettingsManifest demoManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "demo",
                    "show_not_production_banner_enabled", "true")));
    assertThat(BannerUtils.isNotProductionBannerEnabled(demoManifest, fakeRequest())).isFalse();

    SettingsManifest fallbackManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "none",
                    "show_not_production_banner_enabled", "true")));
    assertThat(BannerUtils.isNotProductionBannerEnabled(fallbackManifest, fakeRequest())).isTrue();

    SettingsManifest noneManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "none",
                    "show_not_production_banner_enabled", "false")));
    assertThat(BannerUtils.isNotProductionBannerEnabled(noneManifest, fakeRequest())).isFalse();
  }

  @Test
  public void isDemoBannerEnabled_returnsExpectedValues() {
    SettingsManifest demoManifest =
        new SettingsManifest(ConfigFactory.parseMap(ImmutableMap.of("banner_type", "demo")));
    assertThat(BannerUtils.isDemoBannerEnabled(demoManifest, fakeRequest())).isTrue();

    SettingsManifest notProdManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("banner_type", "not_production")));
    assertThat(BannerUtils.isDemoBannerEnabled(notProdManifest, fakeRequest())).isFalse();

    SettingsManifest noneManifest =
        new SettingsManifest(ConfigFactory.parseMap(ImmutableMap.of("banner_type", "none")));
    assertThat(BannerUtils.isDemoBannerEnabled(noneManifest, fakeRequest())).isFalse();
  }

  @Test
  public void getDemoBannerDaysRemainingCount_returnsExpectedValues() {
    Clock fixedClock = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneId.of("UTC"));

    // Future date (5 days)
    SettingsManifest futureManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-06")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(futureManifest, fixedClock))
        .contains(5L);

    // Future date (1 day)
    SettingsManifest tomorrowManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-02")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(tomorrowManifest, fixedClock))
        .contains(1L);

    // Same day (0 days) -> not in the future, should be empty
    SettingsManifest todayManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-01")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(todayManifest, fixedClock)).isEmpty();

    // Past date (negative)
    SettingsManifest pastManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-08-31")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(pastManifest, fixedClock)).isEmpty();

    // Invalid date format
    SettingsManifest invalidManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026/09/06")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(invalidManifest, fixedClock)).isEmpty();

    // Blank date
    SettingsManifest blankManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "  ")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(blankManifest, fixedClock)).isEmpty();
  }
}
