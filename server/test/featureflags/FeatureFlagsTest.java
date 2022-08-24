package featureflags;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.junit.Test;
import play.mvc.Http.Request;

public class FeatureFlagsTest {

  private static final Config overridesEnabledConfig =
      ConfigFactory.parseMap(ImmutableMap.of("feature_flag_overrides_enabled", "true"));
  private static final Config everythingEnabledConfig =
      ConfigFactory.parseMap(
          ImmutableMap.of(
              "feature_flag_overrides_enabled",
              "true",
              FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED,
              "true"));
  private static final Config featuresEnabledConfig =
      ConfigFactory.parseMap(
          ImmutableMap.of(FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED, "true"));
  private static final Map<String, String> allFeaturesEnabledMap =
      Map.of(FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED, "true");
  private static final Request allFeaturesEnabledRequest =
      fakeRequest().session(allFeaturesEnabledMap).build();
  private static final Map<String, String> allFeaturesDisabledMap =
      Map.of(FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED, "false");
  private static final Request allFeaturesDisabledRequest =
      fakeRequest().session(allFeaturesDisabledMap).build();

  @Test
  public void isStatusTrackingEnabled_withNoConfig_withNoOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());
    assertThat(featureFlags.isStatusTrackingEnabled(fakeRequest().build())).isFalse();
  }

  @Test
  public void isStatusTrackingEnabled_withOverridesDisabled_withOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());
    // Overrides only apply if the config is present.
    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesEnabledRequest)).isFalse();
  }

  @Test
  public void isStatusTrackingEnabled_withFeatureEnabled_withNoOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(featuresEnabledConfig);
    assertThat(featureFlags.isStatusTrackingEnabled(fakeRequest().build())).isTrue();
  }

  @Test
  public void
      isStatusTrackingEnabled_withFeatureUnset_withOverridesEnabled_withOverride_isNotEnabled() {
    // A flag not in the config can not be overriden.
    FeatureFlags featureFlags = new FeatureFlags(overridesEnabledConfig);
    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesEnabledRequest)).isFalse();
  }

  @Test
  public void
      isStatusTrackingEnabled_withFeatureEnabled_withOverridesDisabled_withDisabledOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(featuresEnabledConfig);
    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesDisabledRequest)).isTrue();
  }

  @Test
  public void
      isStatusTrackingEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideFalse_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(everythingEnabledConfig);
    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesDisabledRequest)).isFalse();
  }

  @Test
  public void
      isStatusTrackingEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideTrue_isTrue() {
    FeatureFlags featureFlags = new FeatureFlags(everythingEnabledConfig);
    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesEnabledRequest)).isTrue();
  }
}
