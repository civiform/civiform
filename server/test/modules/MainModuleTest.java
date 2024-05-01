package modules;

import static org.assertj.core.api.Assertions.assertThat;

import annotations.BindingAnnotations.Now;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.Application;
import play.inject.BindingKey;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

@RunWith(JUnitParamsRunner.class)
public class MainModuleTest {

  private static final ZoneId PST_TIME_ZONE = ZoneId.of("America/Los_Angeles");

  // Note: Each test is responsible for configuring an application object
  // since we're varying the configuration values of the application itself.
  private Optional<Application> maybeApp;

  @Before
  public void setUpApp() {
    maybeApp = Optional.empty();
  }

  @After
  public void tearDownApp() {
    if (maybeApp.isPresent()) {
      // Explicitly stopping the generated application object is necessary
      // to prevent leaking a DB connection and causing later tests to fail.
      Helpers.stop(maybeApp.get());
    }
  }

  @Test
  public void testTimeZone_configValueNotSet_DefaultsToPST() {
    Application app = new GuiceApplicationBuilder().build();
    maybeApp = Optional.of(app);

    Clock clock = app.injector().instanceOf(Clock.class);
    assertThat(clock.getZone()).isEqualTo(PST_TIME_ZONE);

    ZoneId zoneId = app.injector().instanceOf(ZoneId.class);
    assertThat(zoneId).isEqualTo(PST_TIME_ZONE);

    BindingKey<LocalDateTime> key =
        new BindingKey<LocalDateTime>(LocalDateTime.class).qualifiedWith(Now.class);
    LocalDateTime now = app.injector().instanceOf(key.asScala());
    assertThat(now.atZone(zoneId)).isEqualTo(now.atZone(PST_TIME_ZONE));
  }

  @Test
  @Parameters({"America/Los_Angeles", "America/New_York", "America/Chicago"})
  public void testTimeZone_configValueProvided(String timeZone) {
    Application app =
        new GuiceApplicationBuilder()
            .configure(ConfigFactory.parseMap(ImmutableMap.of("civiform_time_zone_id", timeZone)))
            .build();
    maybeApp = Optional.of(app);

    Clock clock = app.injector().instanceOf(Clock.class);
    assertThat(clock.getZone()).isEqualTo(ZoneId.of(timeZone));

    ZoneId zoneId = app.injector().instanceOf(ZoneId.class);
    assertThat(zoneId).isEqualTo(ZoneId.of(timeZone));

    BindingKey<LocalDateTime> key =
        new BindingKey<LocalDateTime>(LocalDateTime.class).qualifiedWith(Now.class);
    LocalDateTime now = app.injector().instanceOf(key.asScala());
    assertThat(now.atZone(zoneId)).isEqualTo(now.atZone(ZoneId.of(timeZone)));
  }
}
