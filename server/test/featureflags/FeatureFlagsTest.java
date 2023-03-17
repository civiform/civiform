package featureflags;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class FeatureFlagsTest {
  private static final Config overridesEnabledConfig =
      ConfigFactory.parseMap(
          ImmutableMap.of(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.getSymbol(), "true"));

  private static final Map<String, String> allFeaturesDisabledMap =
      Arrays.stream(FeatureFlag.values())
          .filter(
              flag ->
                  !flag.getSymbol().equals(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.getSymbol()))
          .collect(Collectors.toMap(FeatureFlag::getSymbol, unused -> "false"));

  private static final Map<String, String> allFeaturesEnabledMap =
      Arrays.stream(FeatureFlag.values())
          .filter(
              flag ->
                  !flag.getSymbol().equals(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.getSymbol()))
          .collect(Collectors.toMap(FeatureFlag::getSymbol, unused -> "true"));
  private static final Map<String, String> allFeaturesAndOverridesEnabledMap =
      Arrays.stream(FeatureFlag.values())
          .collect(Collectors.toMap(FeatureFlag::getSymbol, unused -> "true"));

  @Test
  public void isEnabled_withNoConfig_withNoOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());

    assertThat(featureFlags.isProgramEligibilityConditionsEnabled(fakeRequest().build())).isFalse();
  }

  @Test
  public void isEnabled_withOverridesDisabled_withOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());

    // Overrides only apply if the config is present.
    assertThat(
            featureFlags.isProgramEligibilityConditionsEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build()))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureDisabled_withNoOverride_isDisables() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesDisabledMap));

    assertThat(featureFlags.isProgramEligibilityConditionsEnabled(fakeRequest().build())).isFalse();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withNoOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));

    assertThat(featureFlags.isProgramEligibilityConditionsEnabled(fakeRequest().build())).isTrue();
  }

  @Test
  public void isEnabled_withFeatureUnset_withOverridesEnabled_withOverride_isNotEnabled() {
    // A flag not in the config can not be overriden.
    FeatureFlags featureFlags = new FeatureFlags(overridesEnabledConfig);

    assertThat(
            featureFlags.isProgramEligibilityConditionsEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build()))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withOverridesDisabled_withDisabledOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));

    assertThat(
            featureFlags.isProgramEligibilityConditionsEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build()))
        .isTrue();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideFalse_isNotEnabled() {
    FeatureFlags featureFlags =
        new FeatureFlags(ConfigFactory.parseMap(allFeaturesAndOverridesEnabledMap));

    assertThat(
            featureFlags.isProgramEligibilityConditionsEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build()))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideTrue_isTrue() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));

    assertThat(
            featureFlags.isProgramEligibilityConditionsEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build()))
        .isTrue();
  }

  @Test
  public void programReadOnlyViewEnabled_withNoConfig_withNoOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());
    assertThat(featureFlags.isReadOnlyProgramViewEnabled(fakeRequest().build())).isFalse();
  }

  @Test
  public void programReadOnlyViewEnabled_withOverridesDisabled_withOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());
    // Overrides only apply if the config is present.
    assertThat(
            featureFlags.isReadOnlyProgramViewEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build()))
        .isFalse();
  }

  @Test
  public void programReadOnlyViewEnabled_withFeatureEnabled_withNoOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));
    assertThat(featureFlags.isReadOnlyProgramViewEnabled(fakeRequest().build())).isTrue();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureUnset_withOverridesEnabled_withOverride_isNotEnabled() {
    // A flag not in the config can not be overriden.
    FeatureFlags featureFlags = new FeatureFlags(overridesEnabledConfig);
    assertThat(
            featureFlags.isReadOnlyProgramViewEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build()))
        .isFalse();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureEnabled_withOverridesDisabled_withDisabledOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));
    assertThat(
            featureFlags.isReadOnlyProgramViewEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build()))
        .isTrue();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideFalse_isNotEnabled() {
    FeatureFlags featureFlags =
        new FeatureFlags(ConfigFactory.parseMap(allFeaturesAndOverridesEnabledMap));
    assertThat(
            featureFlags.isReadOnlyProgramViewEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build()))
        .isFalse();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideTrue_isTrue() {
    FeatureFlags featureFlags =
        new FeatureFlags(ConfigFactory.parseMap(allFeaturesAndOverridesEnabledMap));
    assertThat(
            featureFlags.isReadOnlyProgramViewEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build()))
        .isTrue();
  }

  @Test
  public void allowCiviformAdminAccessPrograms_isTrue() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));
    assertThat(featureFlags.allowCiviformAdminAccessPrograms(fakeRequest().build())).isTrue();
  }
}
