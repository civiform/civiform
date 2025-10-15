package repository;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.DB;
import io.ebean.Database;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.ApplicationLifecycle;

@Singleton
public final class SettingsCacheMaintainer {

  private static final Logger logger = LoggerFactory.getLogger(SettingsCacheMaintainer.class);

  @VisibleForTesting static final String CHANNEL = "settings_update";
  private static final int NOTIFY_TIMEOUT_MS = 500;

  private final SettingsGroupRepository repo;
  // Need for mocking unit tests, rather than the static DB.getDefault(),
  private final Database database;
  private final ApplicationLifecycle lifecycle;

  private Thread listenerThread;
  private volatile boolean running = true;

  @Inject
  public SettingsCacheMaintainer(SettingsGroupRepository repo, ApplicationLifecycle lifecycle) {
    this(repo, DB.getDefault(), lifecycle);
  }

  @VisibleForTesting
  SettingsCacheMaintainer(
      SettingsGroupRepository repo, Database database, ApplicationLifecycle lifecycle) {
    this.repo = repo;
    this.database = database;
    this.lifecycle = lifecycle;
  }

  public void init() {
    startNotifyListener();

    // On application shutdown, stop the listener thread
    lifecycle.addStopHook(
        () -> {
          logger.info("Shutting down SettingsCacheMaintainer");
          running = false;
          if (listenerThread != null) {
            listenerThread.interrupt();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  /** Spins up a dedicated thread that listens for Postgres notifications. */
  private void startNotifyListener() {
    listenerThread =
        new Thread(
            () -> {
              while (running) {
                try (Connection conn = database.dataSource().getConnection();
                    Statement stmt = conn.createStatement()) {

                  // Immediately register the listener
                  conn.setAutoCommit(true);
                  stmt.execute("LISTEN " + CHANNEL);
                  logger.info("Listening on '{}' (autocommit={})", CHANNEL, conn.getAutoCommit());

                  // Clear the cache each time the listener connects successfully, to ensure
                  // consistency in case of an update while the listener was disconnected.
                  repo.clearCurrentSettingsCache();

                  PGConnection pgConn = conn.unwrap(PGConnection.class);

                  // Wait for notifications, unblocking every NOTIFY_TIMEOUT_MS to allow
                  // for a prompt shutdown.
                  while (running) {
                    // logger.info("Waiting for notifications");
                    PGNotification[] notifications = pgConn.getNotifications(NOTIFY_TIMEOUT_MS);
                    if (notifications != null && notifications.length > 0) {
                      logger.debug(
                          "Received {} notification(s) on '{}'", notifications.length, CHANNEL);
                      repo.clearCurrentSettingsCache();
                    }
                  }
                } catch (PSQLException e) {
                  Throwable cause = e.getCause();
                  if (cause instanceof SocketException
                      && cause.getMessage().contains("Socket closed")) {
                    logger.info(
                        "SettingsCache listener socket closed. Will reconnect unless application is"
                            + " shutting down.");
                  } else {
                    logger.error("SettingsCache listener PSQLException — retrying in 1s", e);
                    try {
                      Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                      // In case we shut down while we're waiting to retry
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }
                } catch (Throwable t) {
                  // Catch everything so we don't lose the listener
                  logger.error("SettingsCache listener crashed — retrying in 1s", t);
                  try {
                    Thread.sleep(1000);
                  } catch (InterruptedException ie) {
                    // In case we shut down while we're waiting to retry
                    Thread.currentThread().interrupt();
                    break;
                  }
                }
              }
            },
            "SettingsCache-Listener");

    // Make this a daemon thread so it doesn't block the JVM from shutting down
    // should something go wrong with it.
    listenerThread.setDaemon(true);
    listenerThread.start();
    logger.info("SettingsCache listener thread started");
  }
}
