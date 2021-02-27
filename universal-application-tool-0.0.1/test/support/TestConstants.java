package support;

import com.google.common.collect.ImmutableMap;

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
}
