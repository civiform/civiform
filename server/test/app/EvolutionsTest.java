package app;

import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Before;
import org.junit.Test;
import play.api.db.evolutions.InconsistentDatabase;
import play.db.Database;
import play.db.Databases;
import play.db.evolutions.Evolutions;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithApplication;

/**
 * This will create a new empty postgres database and run all the evolutions first doing all ups,
 * then attempting to do all downs.
 *
 * <p>Any bad ups will cause the test to fail
 *
 * <p>Bad downs will cause the test to fail only for revisions over 78.sql due to a number of poorly
 * written downs
 */
public class EvolutionsTest extends WithApplication {

  private static final String tempDatabaseName = "evolutionsdb";
  private Config config;
  private Database database;

  @Override
  protected play.Application provideApplication() {
    return new GuiceApplicationBuilder()
        .configure("play.evolutions.db.default.enabled", false)
        .configure("play.evolutions.db.default.autoApply", false)
        .configure("play.evolutions.db.default.autoApplyDowns", false)
        .build();
  }

  @Before
  public void setUp() {
    config = app.injector().instanceOf(Config.class);
    database = app.injector().instanceOf(Database.class);
  }

  @Test
  public void testApplyAndCleanupOfAllEvolutions() {
    // In case it didn't get cleaned up
    dropDatabase();
    createDatabase();
    Database newDatabase = newDatabaseConnection();

    // Apply all ups
    try {
      Evolutions.applyEvolutions(newDatabase);
    } catch (InconsistentDatabase ex) {
      closeAndDropDatabase(newDatabase);
      fail(ex.script());
    }

    // Apply all downs
    try {
      Evolutions.cleanupEvolutions(newDatabase);
    } catch (InconsistentDatabase ex) {
      // Fail test if any down evolutions over 78.sql fail. We'll only check for new
      // ones going forward since there are a number of files 78 and under that have
      // bad downs. :(
      if (ex.rev() > 78) {
        closeAndDropDatabase(newDatabase);
        fail(ex.script());
      }
    }

    // Clean up
    closeAndDropDatabase(newDatabase);
  }

  /** Creates a database if it doesn't exist */
  private void createDatabase() {
    try (Connection connection = database.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        // No `if not exists` for dropping databases in postgres so fake it
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + tempDatabaseName + "'");

        if (!resultSet.next()) {
          statement.execute("CREATE DATABASE " + tempDatabaseName);
          statement.execute(
              "GRANT ALL PRIVILEGES ON DATABASE " + tempDatabaseName + " TO postgres;");
        }
      }
    } catch (SQLException ex) {
      fail(ex.getMessage());
    }
  }

  /** Closes the database connection and drops the database if it exists */
  private void closeAndDropDatabase(Database newDatabase) {
    newDatabase.shutdown();
    dropDatabase();
  }

  /** Drops the database if it exists */
  private void dropDatabase() {
    try (Connection connection = database.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        // No `if exists` for dropping databases in postgres so fake it
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + tempDatabaseName + "'");

        if (resultSet.next()) {
          statement.execute("DROP DATABASE " + tempDatabaseName);
        }
      }
    } catch (SQLException ex) {
      fail(ex.getMessage());
    }
  }

  /** Create a new connection to the newly created database */
  private Database newDatabaseConnection() {
    // Quick and dirty jdbc url to swap the tempDatabaseName. It's unlikely
    // the format will need to change change. This will be good enough.
    String[] parts = database.getUrl().split("/");
    parts[parts.length - 1] = tempDatabaseName;
    String url = String.join("/", parts);

    return Databases.createFrom(
        "org.postgresql.Driver",
        url,
        ImmutableMap.<String, Object>builder()
            .put("username", config.getString("db.default.username"))
            .put("password", config.getString("db.default.password"))
            .build());
  }
}
