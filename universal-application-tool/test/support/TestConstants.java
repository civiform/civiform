package support;

import com.google.common.collect.ImmutableMap;
import play.api.test.Helpers;

public class TestConstants {

  public static final ImmutableMap<String, Object> TEST_DATABASE_CONFIG =
      ImmutableMap.of(
          "db.default.driver",
          "org.testcontainers.jdbc.ContainerDatabaseDriver",
          "db.default.url",
          /* This is a magic string.  The components of it are
           * jdbc: the standard java database connection uri scheme
           * tc: Testcontainers - the tool that starts a new container per test.
           * postgresql: which container to start
           * 12.5: which version of postgres to start
           * ///: hostless URI scheme - anything here would be ignored
           * databasename: the name of the db to connect to - any string is okay.
           */
          "jdbc:tc:postgresql:12.5:///databasename",
          "play.evolutions.db.default.enabled ",
          "true");

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
