package featureflags;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.mvc.Http.Request;

@RunWith(JUnitParamsRunner.class)
public class FeatureFlagsTest {

  private static final List<FeatureFlag> ALL_FEATURE_FLAGS =
      Arrays.stream(FeatureFlag.values())
          // Exclude the override flag because it is not a true feature.
          .filter(flag -> flag != FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED)
          .collect(Collectors.toList());

  // When unit test methods specify this named parameter, they will set their
  // FeatureFlag parameter to every possible value except
  // FEATURE_FLAG_OVERRIDES_ENABLED.
  private static final String ALL_FEATURE_FLAGS_LIST = "allFlagsExceptOverrideList";

  @NamedParameters(ALL_FEATURE_FLAGS_LIST)
  private static List<FeatureFlag> allFeatureFlagsList() {
    return ALL_FEATURE_FLAGS;
  }

  /** Helper method to test the FeatureFlags::getFlagEnabled method based on specific situations. */
  private static boolean testGetFlagEnabled(
      boolean configHasFlagsEnabled,
      boolean configHasOverridesEnabled,
      boolean requestHasFlagOverrides,
      FeatureFlag flag) {
    ImmutableMap.Builder<String, String> configMap = ImmutableMap.builder();
    if (configHasFlagsEnabled) {
      ALL_FEATURE_FLAGS.forEach(featureFlag -> configMap.put(featureFlag.toString(), "true"));
    }
    if (configHasOverridesEnabled) {
      configMap.put(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString(), "true");
    }
    FeatureFlags featureFlags = new FeatureFlags(ConfigFactory.parseMap(configMap.build()));

    Request request;
    if (requestHasFlagOverrides) {
      request =
          fakeRequest()
              .session(
                  ALL_FEATURE_FLAGS.stream()
                      .collect(Collectors.toMap(FeatureFlag::toString, unused -> "true")))
              .build();
    } else {
      request = fakeRequest().build();
    }

    return featureFlags.getFlagEnabled(request, flag);
  }

  @Test
  @Parameters(named = ALL_FEATURE_FLAGS_LIST)
  public void isEnabled_correctly(FeatureFlag flag) {
    // The flag isn't mentioned anywhere, so it should be false.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ false,
                /*configHasOverridesEnabled=*/ false,
                /*requestHasFlagOverrides=*/ false,
                flag))
        .isFalse();

    // The flag is set to true in the config, so it should be true.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ true,
                /*configHasOverridesEnabled=*/ false,
                /*requestHasFlagOverrides=*/ false,
                flag))
        .isTrue();

    // The flag is not set in the config despite overrides being set, so it should be false.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ false,
                /*configHasOverridesEnabled=*/ true,
                /*requestHasFlagOverrides=*/ false,
                flag))
        .isFalse();

    // The request overrides the flag to true, but overrides are disabled.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ false,
                /*configHasOverridesEnabled=*/ false,
                /*requestHasFlagOverrides=*/ true,
                flag))
        .isFalse();

    // The request overrides the flag to true, and overrides are enabled.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ true,
                /*configHasOverridesEnabled=*/ true,
                /*requestHasFlagOverrides=*/ false,
                flag))
        .isTrue();

    // The config set the flag to true. The request does as well, but overrides are
    // disabled so this is a no-op.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ true,
                /*configHasOverridesEnabled=*/ false,
                /*requestHasFlagOverrides=*/ true,
                flag))
        .isTrue();

    // A flag not in the config can not be overriden.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ false,
                /*configHasOverridesEnabled=*/ true,
                /*requestHasFlagOverrides=*/ true,
                flag))
        .isFalse();

    // The config set the flag to true. The request does as well, and overrides are enabled.
    assertThat(
            testGetFlagEnabled(
                /*configHasFlagsEnabled=*/ true,
                /*configHasOverridesEnabled=*/ true,
                /*requestHasFlagOverrides=*/ true,
                flag))
        .isTrue();
  }
}
