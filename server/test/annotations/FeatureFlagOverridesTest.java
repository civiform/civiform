package annotations;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

public class FeatureFlagOverridesTest {
  private static final String FLAG_NAME = "flag_name";
  private static final Config overridesEnabledConfig =
      ConfigFactory.parseMap(ImmutableMap.of("feature_flag_overrides_enabled", "true"));

  private FeatureFlagOverrides overrides;

  @Before
  public void setUp() {
    overrides = new FeatureFlagOverrides(overridesEnabledConfig);
  }

  @Test
  public void override() {
    overrides.setOverride(FLAG_NAME, "true");
    assertThat(overrides.getOverrideBoolean(FLAG_NAME)).contains(true);
  }

  @Test
  public void setOverrides_sameFlagTwice() {
    overrides.setOverride(FLAG_NAME, "true");
    assertThat(overrides.getOverrideBoolean(FLAG_NAME)).contains(true);

    overrides.setOverride(FLAG_NAME, "false");
    assertThat(overrides.getOverrideBoolean(FLAG_NAME)).contains(false);
  }

  @Test
  public void getOverrides_unknownFlag() {
    assertThat(overrides.getOverrideBoolean("unknown_flag")).isEmpty();
  }

  @Test
  public void getOverride_overridesDisabled() {
    overrides =
        new FeatureFlagOverrides(
            ConfigFactory.parseMap(ImmutableMap.of("feature_flag_overrides_enabled", "false")));
    overrides.setOverride(FLAG_NAME, "true");

    assertThat(overrides.getOverrideBoolean(FLAG_NAME)).isEmpty();
  }

  @Test
  public void global_storage() {
    // All instances share the same settings.
    FeatureFlagOverrides overridesTwo = new FeatureFlagOverrides(overridesEnabledConfig);
    overrides.setOverride(FLAG_NAME, "true");

    assertThat(overridesTwo.getOverrideBoolean(FLAG_NAME)).contains(true);
  }
}
