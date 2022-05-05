package app.modules;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.time.Clock;
import java.time.ZoneId;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

@RunWith(JUnitParamsRunner.class)
public class MainModuleTest {

  private Application app;

  @After
  public void tearDownApp() {
    if (app != null) {
      Helpers.stop(app);
    }
  }

  @Test
  public void testTimeZone_configValueNotSet_DefaultsToPST() {
    app = new GuiceApplicationBuilder().build();
    Clock clock = app.injector().instanceOf(Clock.class);
    assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
  }

  @Test
  @Parameters({"America/Los_Angeles", "America/New_York", "America/Chicago"})
  public void testTimeZone_configValueProvided(String timeZone) {
    app =
        new GuiceApplicationBuilder()
            .configure(ConfigFactory.parseMap(ImmutableMap.of("civiform.time.zoneid", timeZone)))
            .build();
    Clock clock = app.injector().instanceOf(Clock.class);
    assertThat(clock.getZone()).isEqualTo(ZoneId.of(timeZone));
  }
}
