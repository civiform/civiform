package support;

import static org.mockito.Mockito.mockStatic;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.mockito.MockedStatic;
import play.api.test.Helpers;

public class CfTestHelpers {

  // ErrorProne raises a warning about the return value from
  // mockedStatic.when(Instant::now).thenReturn(instant)
  // not being used unless suppressed.
  @SuppressWarnings("ReturnValueIgnored")
  public static void withMockedInstantNow(String instantString, Runnable fn) {
    Clock clock = Clock.fixed(Instant.parse(instantString), ZoneId.of("UTC"));
    Instant instant = Instant.now(clock);

    try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
      mockedStatic.when(Instant::now).thenReturn(instant);
      fn.run();
    }
  }

  public static ImmutableMap<String, Object> oidcConfig(String host, int port) {
    return ImmutableMap.of(
        "idcs.client_id",
        "foo",
        "idcs.secret",
        "bar",
        "idcs.discovery_uri",
        String.format("http://%s:%d/.well-known/openid-configuration", host, port),
        "base_url",
        String.format("http://localhost:%d", Helpers.testServerPort()));
  }
}
