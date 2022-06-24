package modules;

import static org.assertj.core.api.Assertions.assertThat;

import annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.Application;
import play.inject.BindingKey;
import play.inject.guice.GuiceApplicationBuilder;

@RunWith(JUnitParamsRunner.class)
public class FeatureFlagsModuleTest {

  @Test
  @Parameters({"false", "true"})
  public void testFlag(boolean isEnabled) {
    Application app =
        new GuiceApplicationBuilder()
            .configure(
                ConfigFactory.parseMap(ImmutableMap.of("application_status_tracking_enabled", isEnabled)))
            .build();
    BindingKey<Boolean> key =
        new BindingKey<Boolean>(Boolean.class)
            .qualifiedWith(ApplicationStatusTrackingEnabled.class);

    Boolean actualState = app.injector().instanceOf(key.asScala());

    assertThat(actualState).isEqualTo(isEnabled);
  }
}
