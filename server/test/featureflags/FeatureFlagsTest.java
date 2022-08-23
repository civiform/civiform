package featureflags;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;

public class FeatureFlagsTest {

  private static final Config overridesEnabledConfig =
      ConfigFactory.parseMap(ImmutableMap.of("feature_flag_overrides_enabled", "true"));
  private static final Map<String, String> allFeaturesEnabledMap =
      Map.of(FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED, "true");
  private static final Request allFeaturesEnabledRequest =
      fakeRequest().session(allFeaturesEnabledMap).build();

  private FeatureFlags featureFlags;

  @Before
  public void setUp() {
    featureFlags = new FeatureFlags(overridesEnabledConfig);
  }

  @Test
  public void isStatusTrackingEnabled_enabled() {
    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesEnabledRequest)).isTrue();
  }

  @Test
  public void isStatusTrackingEnabled_disabled() {
    assertThat(featureFlags.isStatusTrackingEnabled(fakeRequest().build())).isFalse();
  }

  @Test
  public void isStatusTrackingEnabled_enabled_overridesDisabled() {
    featureFlags =
        new FeatureFlags(
            ConfigFactory.parseMap(ImmutableMap.of("feature_flag_overrides_enabled", "false")));

    assertThat(featureFlags.isStatusTrackingEnabled(allFeaturesEnabledRequest)).isFalse();
  }
}
