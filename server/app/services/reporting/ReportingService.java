package services.reporting;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import repository.ReportingRepository;

/** A service responsible for logic related to collating and presenting reporting data. */
public final class ReportingService {

  private final ReportingRepository reportingRepository;

  @Inject
  public ReportingService(ReportingRepository reportingRepository) {
    this.reportingRepository = Preconditions.checkNotNull(reportingRepository);
  }

  public MonthlyStats getMonthlyStats() {
    ImmutableList<ApplicationSubmissionsStat> submissionsByProgramByMonth =
        reportingRepository.loadMonthlyReportingView();

    return MonthlyStats.create(
        monthlySubmissionsAggregated(submissionsByProgramByMonth),
        totalSubmissionsByProgram(submissionsByProgramByMonth));
  }

  /** Monthly application submission stats for all programs. */
  public ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsAggregated(
      ImmutableList<ApplicationSubmissionsStat> submissionsByProgramByMonth) {
    Map<Timestamp, ApplicationSubmissionsStat.Aggregator> aggregators = new HashMap<>();

    for (var stat : submissionsByProgramByMonth) {
      ApplicationSubmissionsStat.Aggregator aggregator =
          aggregators.containsKey(stat.timestamp())
              ? aggregators.get(stat.timestamp())
              : aggregators.put(
                  stat.timestamp(),
                  new ApplicationSubmissionsStat.Aggregator("All", stat.timestamp()));

      aggregator.update(stat);
    }

    return aggregators.values().stream()
        .map(ApplicationSubmissionsStat.Aggregator::getAggregateStat)
        .collect(ImmutableList.toImmutableList());
  }

  /** Total application submission stats for each program. */
  public ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram(
      ImmutableList<ApplicationSubmissionsStat> submissionsByProgramByMonth) {
    Map<String, ApplicationSubmissionsStat.Aggregator> aggregators = new HashMap<>();

    for (var stat : submissionsByProgramByMonth) {
      ApplicationSubmissionsStat.Aggregator aggregator =
          aggregators.containsKey(stat.programName())
              ? aggregators.get(stat.programName())
              : aggregators.put(
                  stat.programName(),
                  new ApplicationSubmissionsStat.Aggregator(
                      stat.programName(), Timestamp.valueOf("0")));

      aggregator.update(stat);
    }

    return aggregators.values().stream()
        .map(ApplicationSubmissionsStat.Aggregator::getAggregateStat)
        .collect(ImmutableList.toImmutableList());
  }

  @AutoValue
  public abstract static class MonthlyStats {

    public static MonthlyStats create(
        ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsAggregated,
        ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram) {
      return new AutoValue_ReportingService_MonthlyStats(
          monthlySubmissionsAggregated, totalSubmissionsByProgram);
    }

    public abstract ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsAggregated();

    public abstract ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram();
  }
}
