package app.modules;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;

@RunWith(JUnitParamsRunner.class)
public class MainModuleTest {
  
  @Test
  @Parameters({
    "America/Los_Angeles",
    "America/New_York"
  })
  public void testTimeZone(String timeZone) {
    Application app = new GuiceApplicationBuilder()
      .configure(ConfigFactory.parseMap(ImmutableMap.of("java.time.zoneid", timeZone)))
      .build();
    Clock clock = app.injector().instanceOf(Clock.class);
    assertThat(clock.getZone()).isEqualTo(ZoneId.of(timeZone));
  }
}
