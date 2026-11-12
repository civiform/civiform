package views.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;
import services.settings.SettingsManifest;

public class DemoBannerTest extends ResetPostgres {
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneId.of("America/Los_Angeles"));

  @Test
  public void create_populatesBannerFieldsAndDaysRemainingWhenEnabled() {
    SettingsManifest manifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "demo",
                    "whitelabel_civic_entity_short_name", "TestCity",
                    "demo_banner_expiration_date", "2026-09-06",
                    "staging_disable_demo_mode_logins", "false")));

    DemoBanner banner =
        DemoBanner.create(manifest, fakeRequest(), Optional.empty(), true, FIXED_CLOCK);

    assertThat(banner.enabled()).isTrue();
    assertThat(banner.showDebugTools()).isTrue();
    assertThat(banner.civicEntityShortName()).isEqualTo("TestCity");
    assertThat(banner.daysRemainingCount()).contains(5L);
    assertThat(banner.isResident()).isTrue();
    assertThat(banner.isAdminOrTi()).isFalse();
  }

  @Test
  public void create_hidesDebugToolsWhenNotDevOrStagingOrWhenDemoLoginsDisabled() {
    SettingsManifest enabledLoginsManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "demo",
                    "staging_disable_demo_mode_logins", "false")));
    assertThat(
            DemoBanner.create(
                    enabledLoginsManifest, fakeRequest(), Optional.empty(), false, FIXED_CLOCK)
                .showDebugTools())
        .isFalse();

    SettingsManifest disabledLoginsManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "demo",
                    "staging_disable_demo_mode_logins", "true")));
    assertThat(
            DemoBanner.create(
                    disabledLoginsManifest, fakeRequest(), Optional.empty(), true, FIXED_CLOCK)
                .showDebugTools())
        .isFalse();
  }

  @Test
  public void create_setsRoleFlagsForProfiles() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    SettingsManifest manifest =
        new SettingsManifest(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "banner_type", "demo",
                    "staging_disable_demo_mode_logins", "false")));

    CiviFormProfile dualAdmin =
        profileFactory.wrapProfileData(profileFactory.createFakeDualAdmin());
    DemoBanner dualBanner =
        DemoBanner.create(manifest, fakeRequest(), Optional.of(dualAdmin), true, FIXED_CLOCK);
    assertThat(dualBanner.isCiviFormAdmin()).isTrue();
    assertThat(dualBanner.isOnlyProgramAdmin()).isFalse();
    assertThat(dualBanner.isAdminOrTi()).isTrue();
    assertThat(dualBanner.isResident()).isFalse();

    CiviFormProfile tiProfile =
        profileFactory.wrapProfileData(profileFactory.createFakeTrustedIntermediary());
    DemoBanner tiBanner =
        DemoBanner.create(manifest, fakeRequest(), Optional.of(tiProfile), true, FIXED_CLOCK);
    assertThat(tiBanner.isTrustedIntermediary()).isTrue();
    assertThat(tiBanner.isAdminOrTi()).isTrue();
    assertThat(tiBanner.isResident()).isFalse();
  }
}
