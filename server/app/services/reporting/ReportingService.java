package services.reporting;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.admin.reporting.AdminReportingIndexView.APPLICATION_COUNTS_BY_MONTH_HEADERS;
import static views.admin.reporting.AdminReportingIndexView.APPLICATION_COUNTS_BY_PROGRAM_HEADERS;
import static views.admin.reporting.AdminReportingShowView.APPLICATION_COUNTS_FOR_PROGRAM_BY_MONTH_HEADERS;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import repository.ReportingRepository;
import services.DateConverter;
import views.admin.reporting.ReportingTableRenderer;

/** A service responsible for logic related to collating and presenting reporting data. */
public final class ReportingService {

  private static final String MONTHLY_REPORTING_DATA_CACHE_KEY = "monthly-reporting-data";
  private static final int MONTHLY_REPORTING_DATA_CACHE_TTL_SECONDS = 60 * 60;

  private final ReportingRepository reportingRepository;
  private final SyncCacheApi reportingDataCache;
  private final DateConverter dateConverter;

  @Inject
  public ReportingService(
      DateConverter dateConverter,
      ReportingRepository reportingRepository,
      @NamedCache("monthly-reporting-data") SyncCacheApi reportingDataCache) {
    this.dateConverter = checkNotNull(dateConverter);
    this.reportingRepository = Preconditions.checkNotNull(reportingRepository);
    this.reportingDataCache = Preconditions.checkNotNull(reportingDataCache);
  }

  /**
   * Application stats in two groups: one grouped by program, one grouped by submission month.
   *
   * <p>Historic monthly stats are stored in a postgres materialized view, but stats for the current
   * month are queried directly in the database. Since that is an unbounded number of rows to scan
   * we cache the result in server memory.
   */
  public MonthlyStats getMonthlyStats() {
    return reportingDataCache.getOrElseUpdate(
        MONTHLY_REPORTING_DATA_CACHE_KEY,
        this::queryAndCollateMonthlyStats,
        MONTHLY_REPORTING_DATA_CACHE_TTL_SECONDS);
  }

  /** The applications by month reporting view as a CSV. */
  public String applicationCountsByMonthCsv() {
    return buildCsv(
        getMonthlyStats().monthlySubmissionsAggregated(),
        APPLICATION_COUNTS_BY_MONTH_HEADERS,
        (printer, stat) -> {
          try {
            printer.print(dateConverter.renderAsTwoDigitMonthAndYear(stat.timestamp().get()));
            printer.print(stat.applicationCount());
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds25p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds50p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds75p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds99p()));

            printer.println();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /** The applications by program reporting view as a CSV. */
  public String applicationCountsByProgramCsv() {
    return buildCsv(
        getMonthlyStats().totalSubmissionsByProgram(),
        APPLICATION_COUNTS_BY_PROGRAM_HEADERS,
        (printer, stat) -> {
          try {
            printer.print(stat.programName());
            printer.print(stat.applicationCount());
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds25p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds50p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds75p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds99p()));

            printer.println();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /** Applications to an individual program by month as a CSV. */
  public String applicationsToProgramByMonthCsv(String programName) {
    return buildCsv(
        getMonthlyStats().monthlySubmissionsForProgram(programName),
        APPLICATION_COUNTS_FOR_PROGRAM_BY_MONTH_HEADERS,
        (printer, stat) -> {
          try {
            printer.print(dateConverter.renderAsTwoDigitMonthAndYear(stat.timestamp().get()));
            printer.print(stat.applicationCount());
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds25p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds50p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds75p()));
            printer.print(
                ReportingTableRenderer.renderDuration(stat.submissionDurationSeconds99p()));

            printer.println();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private String buildCsv(
      ImmutableList<ApplicationSubmissionsStat> stats,
      ImmutableList<ReportingTableRenderer.ReportingTableHeader> headers,
      BiConsumer<CSVPrinter, ApplicationSubmissionsStat> printFn) {
    OutputStream inMemoryBytes = new ByteArrayOutputStream();

    String[] csvHeaders =
        headers.stream()
            .map(ReportingTableRenderer.ReportingTableHeader::headerText)
            .toArray(String[]::new);

    try (Writer writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8)) {
      var printer =
          new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(csvHeaders).build());

      for (var stat : stats) {
        printFn.accept(printer, stat);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return inMemoryBytes.toString();
  }

  private MonthlyStats queryAndCollateMonthlyStats() {
    ImmutableList<ApplicationSubmissionsStat> submissionsByProgramByMonth =
        reportingRepository.loadMonthlyReportingView();
    ImmutableList<ApplicationSubmissionsStat> submissionsThisMonth =
        reportingRepository.loadThisMonthReportingData();

    return MonthlyStats.create(
        Stream.concat(submissionsByProgramByMonth.stream(), submissionsThisMonth.stream())
            .collect(ImmutableList.toImmutableList()),
        monthlySubmissionsAggregated(submissionsByProgramByMonth, submissionsThisMonth),
        totalSubmissionsByProgram(submissionsByProgramByMonth, submissionsThisMonth));
  }

  /** Monthly application submission stats for all programs. */
  private ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsAggregated(
      ImmutableList<ApplicationSubmissionsStat> submissionsByProgramByMonth,
      ImmutableList<ApplicationSubmissionsStat> submissionsThisMonth) {
    Map<Timestamp, ApplicationSubmissionsStat.Aggregator> aggregators = new HashMap<>();

    Stream.concat(submissionsByProgramByMonth.stream(), submissionsThisMonth.stream())
        .forEach(
            stat -> {
              ApplicationSubmissionsStat.Aggregator aggregator;
              if (aggregators.containsKey(stat.timestamp().get())) {
                aggregator = aggregators.get(stat.timestamp().get());
              } else {
                aggregator =
                    new ApplicationSubmissionsStat.Aggregator("All", stat.timestamp().get());
                aggregators.put(stat.timestamp().get(), aggregator);
              }

              aggregator.update(stat);
            });

    return aggregators.values().stream()
        .map(ApplicationSubmissionsStat.Aggregator::getAggregateStat)
        .sorted(Comparator.comparing(stat -> stat.timestamp().get()))
        .collect(ImmutableList.toImmutableList());
  }

  /** Total application submission stats for each program. */
  private ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram(
      ImmutableList<ApplicationSubmissionsStat> submissionsByProgramByMonth,
      ImmutableList<ApplicationSubmissionsStat> submissionsThisMonth) {
    Map<String, ApplicationSubmissionsStat.Aggregator> aggregators = new HashMap<>();

    Stream.concat(submissionsByProgramByMonth.stream(), submissionsThisMonth.stream())
        .forEach(
            stat -> {
              ApplicationSubmissionsStat.Aggregator aggregator;
              if (aggregators.containsKey(stat.programName())) {
                aggregator = aggregators.get(stat.programName());
              } else {
                aggregator = new ApplicationSubmissionsStat.Aggregator(stat.programName());
                aggregators.put(stat.programName(), aggregator);
              }

              aggregator.update(stat);
            });

    return aggregators.values().stream()
        .map(ApplicationSubmissionsStat.Aggregator::getAggregateStat)
        .collect(ImmutableList.toImmutableList());
  }

  /** Application submission stats. */
  @AutoValue
  public abstract static class MonthlyStats {

    public static MonthlyStats create(
        ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsByProgram,
        ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsAggregated,
        ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram) {
      return new AutoValue_ReportingService_MonthlyStats(
          monthlySubmissionsByProgram, monthlySubmissionsAggregated, totalSubmissionsByProgram);
    }

    public abstract ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsByProgram();

    public abstract ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsAggregated();

    public abstract ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram();

    public ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsForProgram(
        String programName) {
      return monthlySubmissionsByProgram().stream()
          .filter(stat -> stat.programName().equals(programName))
          .collect(ImmutableList.toImmutableList());
    }
  }
}
