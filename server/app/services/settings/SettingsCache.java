package services.settings;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.Database;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import models.SettingsGroupModel;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.ApplicationLifecycle;
import repository.DatabaseExecutionContext;
import repository.SettingsGroupRepository;

@Singleton
public final class SettingsCache {

  private static final Logger logger = LoggerFactory.getLogger(SettingsCache.class);

  @VisibleForTesting static final String CHANNEL = "settings_update";
  private static final int NOTIFY_TIMEOUT_MS = 500;

  private final SettingsGroupRepository repo;
  private final DatabaseExecutionContext dbExecutionContext;
  // Need for mocking unit tests, rather than the static DB.getDefault(),
  private final Database database;

  // Volatile so updates are immediately visible to all threads
  private volatile Optional<SettingsGroupModel> cache = Optional.empty();

  private Thread listenerThread;
  private volatile boolean running = true;

  @Inject
  public SettingsCache(
      SettingsGroupRepository repo,
      DatabaseExecutionContext dbExecutionContext,
      Database database,
      ApplicationLifecycle lifecycle) {
    this.repo = repo;
    this.dbExecutionContext = dbExecutionContext;
    this.database = database;

    // Load initial settings into cache
    reloadFromDb();

    startNotifyListener();

    // On application shutdown, stop the listener thread
    lifecycle.addStopHook(
        () -> {
          running = false;
          if (listenerThread != null) {
            listenerThread.interrupt();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  /** Returns the current cached settings (may be empty if none yet). */
  public Optional<SettingsGroupModel> get() {
    return cache;
  }

  /** Reloads from the database into the cache asynchronously. */
  private void reloadFromDb() {
    repo.getCurrentSettings()
        .whenCompleteAsync(
            (freshOpt, ex) -> {
              if (ex != null) {
                logger.error("SettingsCache reload failed", ex);
              } else if (freshOpt.isPresent()) {
                cache = freshOpt;
                logger.debug("SettingsCache reloaded from DB");
              }
            },
            dbExecutionContext);
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

                  // TODO: #11042 - Possibly reloadFromDb to ensure consistency on reconnection

                  PGConnection pgConn = conn.unwrap(PGConnection.class);

                  // Wait for notifications, unblocking every NOTIFY_TIMEOUT_MS to allow
                  // for a prompt shutdown.
                  while (running) {
                    // logger.info("Waiting for notifications");
                    PGNotification[] notifications = pgConn.getNotifications(NOTIFY_TIMEOUT_MS);
                    if (notifications != null && notifications.length > 0) {
                      logger.debug(
                          "Received {} notification(s) on '{}'", notifications.length, CHANNEL);
                      reloadFromDb();
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
