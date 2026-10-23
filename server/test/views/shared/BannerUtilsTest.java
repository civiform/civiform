package views.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.time.LocalDate;
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
    LocalDate fixedToday = LocalDate.of(2026, 9, 1);

    // Future date
    SettingsManifest futureManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-06")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(futureManifest, fixedToday))
        .contains(5L);

    // Same day (0 days)
    SettingsManifest todayManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-01")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(todayManifest, fixedToday)).contains(0L);

    // Past date (negative)
    SettingsManifest pastManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-08-31")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(pastManifest, fixedToday)).isEmpty();

    // Invalid date format
    SettingsManifest invalidManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026/09/06")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(invalidManifest, fixedToday)).isEmpty();

    // Blank date
    SettingsManifest blankManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "  ")));
    assertThat(BannerUtils.getDemoBannerDaysRemainingCount(blankManifest, fixedToday)).isEmpty();
  }
}
