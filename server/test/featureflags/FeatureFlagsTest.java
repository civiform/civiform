package featureflags;

import static featureflags.FeatureFlag.ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS;
import static featureflags.FeatureFlag.PROGRAM_READ_ONLY_VIEW_ENABLED;
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
          ImmutableMap.of(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString(), "true"));

  private static final Map<String, String> allFeaturesDisabledMap =
      Arrays.stream(FeatureFlag.values())
          .filter(
              flag ->
                  !flag.toString().equals(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString()))
          .collect(Collectors.toMap(flag -> flag.toString(), unused -> "false"));

  private static final Map<String, String> allFeaturesEnabledMap =
      Arrays.stream(FeatureFlag.values())
          .filter(
              flag ->
                  !flag.toString().equals(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString()))
          .collect(Collectors.toMap(flag -> flag.toString(), unused -> "true"));
  private static final Map<String, String> allFeaturesAndOverridesEnabledMap =
      Arrays.stream(FeatureFlag.values())
          .collect(Collectors.toMap(flag -> flag.toString(), unused -> "true"));

  @Test
  public void isEnabled_withNoConfig_withNoOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().build(), FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isFalse();
  }

  @Test
  public void isEnabled_withOverridesDisabled_withOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());

    // Overrides only apply if the config is present.
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build(),
                FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureDisabled_withNoOverride_isDisables() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesDisabledMap));

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().build(), FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withNoOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().build(), FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isTrue();
  }

  @Test
  public void isEnabled_withFeatureUnset_withOverridesEnabled_withOverride_isNotEnabled() {
    // A flag not in the config can not be overriden.
    FeatureFlags featureFlags = new FeatureFlags(overridesEnabledConfig);

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build(),
                FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withOverridesDisabled_withDisabledOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build(),
                FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isTrue();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideFalse_isNotEnabled() {
    FeatureFlags featureFlags =
        new FeatureFlags(ConfigFactory.parseMap(allFeaturesAndOverridesEnabledMap));

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build(),
                FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isFalse();
  }

  @Test
  public void isEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideTrue_isTrue() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));

    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build(),
                FeatureFlag.PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED))
        .isTrue();
  }

  @Test
  public void programReadOnlyViewEnabled_withNoConfig_withNoOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());
    assertThat(featureFlags.getFlagEnabled(fakeRequest().build(), PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isFalse();
  }

  @Test
  public void programReadOnlyViewEnabled_withOverridesDisabled_withOverride_isNotEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.empty());
    // Overrides only apply if the config is present.
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build(),
                PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isFalse();
  }

  @Test
  public void programReadOnlyViewEnabled_withFeatureEnabled_withNoOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));
    assertThat(featureFlags.getFlagEnabled(fakeRequest().build(), PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isTrue();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureUnset_withOverridesEnabled_withOverride_isNotEnabled() {
    // A flag not in the config can not be overriden.
    FeatureFlags featureFlags = new FeatureFlags(overridesEnabledConfig);
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build(),
                PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isFalse();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureEnabled_withOverridesDisabled_withDisabledOverride_isEnabled() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build(),
                PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isTrue();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideFalse_isNotEnabled() {
    FeatureFlags featureFlags =
        new FeatureFlags(ConfigFactory.parseMap(allFeaturesAndOverridesEnabledMap));
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesDisabledMap).build(),
                PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isFalse();
  }

  @Test
  public void
      programReadOnlyViewEnabled_withFeatureEnabled_withOverridesEnabled_withOverrideTrue_isTrue() {
    FeatureFlags featureFlags =
        new FeatureFlags(ConfigFactory.parseMap(allFeaturesAndOverridesEnabledMap));
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().session(allFeaturesEnabledMap).build(),
                PROGRAM_READ_ONLY_VIEW_ENABLED))
        .isTrue();
  }

  @Test
  public void allowCiviformAdminAccessPrograms_isTrue() {
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(allFeaturesEnabledMap));
    assertThat(
            featureFlags.getFlagEnabled(
                fakeRequest().build(), ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS))
        .isTrue();
  }
}
