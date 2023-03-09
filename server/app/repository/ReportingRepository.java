package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SqlRow;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.inject.Inject;
import org.postgresql.util.PGInterval;
import services.reporting.ApplicationSubmissionsStat;

/** Implements queries related to reporting needs. */
public final class ReportingRepository {

  private final Clock clock;
  private final Database database;

  @Inject
  public ReportingRepository(Clock clock) {
    this.clock = Preconditions.checkNotNull(clock);
    this.database = DB.getDefault();
  }

  /**
   * Loads data from the monthly reporting view. Does not include data from current month because
   * the job to refresh the view is scheduled to run at the beginning of each month. For current
   * month reporting data use {@code loadThisMonthReportingData}.
   */
  public ImmutableList<ApplicationSubmissionsStat> loadMonthlyReportingView() {
    return database
        .sqlQuery(
            "SELECT * FROM monthly_submissions_reporting_view\n"
                + "WHERE monthly_submissions_reporting_view.submit_month < :first_of_month::date")
        .setParameter("first_of_month", getFirstOfMonth())
        .findList()
        .stream()
        .map(
            row ->
                ApplicationSubmissionsStat.create(
                    row.getString("program_name"),
                    Optional.of(row.getTimestamp("submit_month")),
                    row.getLong("count"),
                    getSecondsFromPgIntervalRowValue(row, "p25"),
                    getSecondsFromPgIntervalRowValue(row, "p50"),
                    getSecondsFromPgIntervalRowValue(row, "p75"),
                    getSecondsFromPgIntervalRowValue(row, "p99")))
        .collect(ImmutableList.toImmutableList());
  }

  private Timestamp getFirstOfMonth() {
    return Timestamp.valueOf(
        LocalDateTime.now(clock).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1));
  }

  /** Loads application submission reporting data for current month. */
  public ImmutableList<ApplicationSubmissionsStat> loadThisMonthReportingData() {
    Timestamp firstOfMonth = getFirstOfMonth();

    return database
        .sqlQuery(
            "SELECT\n"
                + "  programs.name AS program_name,\n"
                + "  count(*),\n"
                + "  percentile_cont(0.5) WITHIN GROUP (\n"
                + "    ORDER BY applications.submission_duration) AS p50,\n"
                + "  percentile_cont(0.25) WITHIN GROUP (\n"
                + "    ORDER BY applications.submission_duration) AS p25,\n"
                + "  percentile_cont(0.75) WITHIN GROUP (\n"
                + "    ORDER BY applications.submission_duration) AS p75,\n"
                + "  percentile_cont(0.99) WITHIN GROUP (\n"
                + "    ORDER BY applications.submission_duration) AS p99\n"
                + "FROM applications\n"
                + "INNER JOIN programs ON applications.program_id = programs.id\n"
                + "WHERE applications.lifecycle_stage IN ('active', 'obsolete')\n"
                + "AND applications.submit_time >= date_trunc('month', :current_date::date)\n"
                + "GROUP BY programs.name")
        .setParameter("current_date", firstOfMonth)
        .findList()
        .stream()
        .map(
            row ->
                ApplicationSubmissionsStat.create(
                    row.getString("program_name"),
                    Optional.of(firstOfMonth),
                    row.getLong("count"),
                    getSecondsFromPgIntervalRowValue(row, "p25"),
                    getSecondsFromPgIntervalRowValue(row, "p50"),
                    getSecondsFromPgIntervalRowValue(row, "p75"),
                    getSecondsFromPgIntervalRowValue(row, "p99")))
        .collect(ImmutableList.toImmutableList());
  }

  /** Triggers a refresh of the monthly reporting view. */
  public void refreshMonthlyReportingView() {
    database.sqlUpdate("REFRESH MATERIALIZED VIEW monthly_submissions_reporting_view").execute();
  }

  private static double getSecondsFromPgIntervalRowValue(SqlRow row, String key) {
    Object interval =
        checkNotNull(
            row.get(key),
            String.format("Expected SqlRow to have key %s but not found in %s", key, row));

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
