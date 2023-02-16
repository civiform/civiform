package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SqlRow;
import javax.inject.Inject;
import org.postgresql.util.PGInterval;
import services.reporting.ApplicationSubmissionsStat;

/** Implements queries related to reporting needs. */
public final class ReportingRepository {

  private final Database database;

  @Inject
  public ReportingRepository() {
    this.database = DB.getDefault();
  }

  /** Loads data from the monthly reporting view. */
  public ImmutableList<ApplicationSubmissionsStat> loadMonthlyReportingView() {
    return database.sqlQuery("SELECT * FROM monthly_submissions_reporting_view").findList().stream()
        .map(
            row ->
                ApplicationSubmissionsStat.create(
                    row.getString("program_name"),
                    row.getTimestamp("submit_month"),
                    row.getLong("count"),
                    getSecondsFromPgIntervalRowVAlue(row, "p25"),
                    getSecondsFromPgIntervalRowVAlue(row, "p50"),
                    getSecondsFromPgIntervalRowVAlue(row, "p75"),
                    getSecondsFromPgIntervalRowVAlue(row, "p99")))
        .collect(ImmutableList.toImmutableList());
  }

  /** Triggers a refresh of the monthly reporting view. */
  public void refreshMonthlyReportingView() {
    database.sqlUpdate("REFRESH MATERIALIZED VIEW monthly_submissions_reporting_view").execute();
  }

  private static double getSecondsFromPgIntervalRowVAlue(SqlRow row, String key) {
    Object interval = row.get(key);

    if (interval == null) {
      throw new IllegalStateException(
          String.format("Expected SqlRow to have key %s but not found in %s", key, row));
    }

    if (!(interval instanceof PGInterval)) {
      throw new IllegalStateException(
          String.format(
              "Expected value at %s in SqlRow to be a PgInterval but got %s: %s",
              key, interval.getClass().getName(), interval));
    }

    return intervalToSeconds((PGInterval) interval);
  }

  private static final long SECONDS_PER_YEAR = 31556926L;
  private static final long SECONDS_PER_DAY = 86400L;
  private static final long SECONDS_PER_HOUR = 3600L;
  private static final long SECONDS_PER_MINUTE = 60L;

  private static double intervalToSeconds(PGInterval interval) {
    return interval.getYears() * SECONDS_PER_YEAR
        + interval.getDays() * SECONDS_PER_DAY
        + interval.getHours() * SECONDS_PER_HOUR
        + interval.getMinutes() * SECONDS_PER_MINUTE
        + interval.getSeconds();
  }
}
