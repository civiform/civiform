package support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.LoggerFactory;

/**
 * Counts the SQL statements Ebean sends to the database, split into reads and writes.
 *
 * <p>Ebean's own query metrics only see ORM queries, missing raw SQL and all writes. Instead this
 * listens to the io.ebean.SQL logger, which logs every statement, and classifies each line. The
 * classification depends on Ebean's log format, which EbeanInvariantTest pins.
 *
 * <p>Lines may start with a transaction prefix such as {@code txn[]}. It is stripped and the line
 * is classified by its leading SQL keyword, so the prefix plays no part in deciding read or write.
 *
 * <ul>
 *   <li>Reads start with select or with, e.g. {@code select t0.id from accounts t0; --bind()}.
 *   <li>Writes start with insert, update or delete, e.g. {@code txn[] insert into ...; -- bind()}.
 *   <li>A batch logs its SQL without a bind, then a {@code -- bind(..)} line per row, then a single
 *       {@code -- executeBatch() size:N} line. The batch is one round trip, so only the
 *       executeBatch line is counted.
 * </ul>
 *
 * <p>Note this will not work if we ever parallelize unit tests and is not thread safe.
 */
public final class SqlStatementCounter {

  /** The statements counted since the previous collection. */
  public record SqlCounts(long reads, long writes) {
    public static SqlCounts withReadsAndWrites(long reads, long writes) {
      return new SqlCounts(reads, writes);
    }

    public static SqlCounts withOnlyReads(long reads) {
      return new SqlCounts(reads, 0);
    }

    public static SqlCounts withOnlyWrites(long writes) {
      return new SqlCounts(0, writes);
    }
  }

  private static final String SQL_LOGGER_NAME = "io.ebean.SQL";

  private final Logger sqlLogger = (Logger) LoggerFactory.getLogger(SQL_LOGGER_NAME);
  private final ClassifyingAppender appender = new ClassifyingAppender();
  // Record logger setting to reset after usage
  private Level originalLevel;
  private boolean originalAdditive;
  private boolean running = false;

  /** Starts counting if not already, otherwise does nothing. */
  public void start() {
    if (running) {
      return;
    }
    originalLevel = sqlLogger.getLevel();
    originalAdditive = sqlLogger.isAdditive();
    appender.start();
    sqlLogger.addAppender(appender);
    sqlLogger.setLevel(Level.DEBUG);
    // Keep the SQL out of the test output.
    sqlLogger.setAdditive(false);
    running = true;
  }

  /** Stops counting and restores the logger. */
  public void stop() {
    if (!running) {
      return;
    }
    sqlLogger.detachAppender(appender);
    sqlLogger.setLevel(originalLevel);
    sqlLogger.setAdditive(originalAdditive);
    appender.stop();
    running = false;
  }

  /**
   * Returns the statements counted since the previous call and resets the counts.
   *
   * @throws IllegalStateException if a logged line could not be classified, as that likely means
   *     Ebean's log format changed and the counts can't be trusted.
   */
  public SqlCounts collect() {
    return appender.collect();
  }

  private static final class ClassifyingAppender extends AppenderBase<ILoggingEvent> {
    private long reads = 0;
    private long writes = 0;
    private final List<String> unclassified = new ArrayList<>();

    // AppenderBase.doAppend synchronizes on this, so statements logged from async threads are
    // serialized with each other and with collect().
    @Override
    protected void append(ILoggingEvent event) {
      String message = event.getFormattedMessage();
      String sql = message;
      // Strip a transaction prefix, e.g. the "txn[] " of:
      //   txn[] delete from accounts where id = -1; -- bind(null) rows(0)
      if (sql.startsWith("txn[")) {
        sql = sql.substring(sql.indexOf(']') + 1);
      }
      sql = sql.stripLeading();
      String keyword = sql.toLowerCase(Locale.ROOT);

      if (keyword.startsWith("-- executebatch()")) {
        // A batch being sent, counted as one write however many rows it holds, e.g.
        //   txn[]  -- executeBatch() size:3 sql:insert into accounts (...) values (?,?,...)
        writes++;
      } else if (keyword.startsWith("--")) {
        // The bind values of one row in a batch, e.g.
        //   txn[]  -- bind(false,[],null,null,null,{},null,2026-10-02T17:01:40.758Z,null,null)
      } else if (keyword.startsWith("select") || keyword.startsWith("with")) {
        // A read, from an ORM query or raw SQL, e.g.
        //   select t0.id, ... from accounts t0; --bind() --micros(251)
        //   SELECT id FROM accounts FOR UPDATE; --bind() --micros(27)
        //   WITH x AS (SELECT id FROM accounts) SELECT id FROM x; --bind() --micros(22)
        reads++;
      } else if (keyword.startsWith("insert")
          || keyword.startsWith("update")
          || keyword.startsWith("delete")) {
        // A write sent on its own includes its bind, e.g.
        //   txn[] insert into accounts (...) values (?,?,...); -- bind(false,[],...)
        //   txn[] UPDATE accounts SET email_address = 'a@b.com'; -- bind(null) rows(1)
        // Without a bind this is a batch's SQL, which is counted by its executeBatch line, e.g.
        //   txn[] insert into accounts (...) values (?,?,...)
        if (sql.contains("-- bind(")) {
          writes++;
        }
      } else {
        unclassified.add(message);
      }
    }

    synchronized SqlCounts collect() {
      if (!unclassified.isEmpty()) {
        ImmutableList<String> lines = ImmutableList.copyOf(unclassified);
        unclassified.clear();
        throw new IllegalStateException("Unclassified SQL log lines: " + lines);
      }
      SqlCounts counts = new SqlCounts(reads, writes);
      reads = 0;
      writes = 0;
      return counts;
    }
  }
}
