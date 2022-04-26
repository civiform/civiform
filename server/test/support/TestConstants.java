package support;

import com.google.common.collect.ImmutableMap;
import play.api.test.Helpers;

public class TestConstants {

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
