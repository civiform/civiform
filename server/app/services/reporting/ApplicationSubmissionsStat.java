package services.reporting;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Represents statistics about the number of applications submitted to a given program in a given
 * timeframe.
 */
@AutoValue
public abstract class ApplicationSubmissionsStat {

  public static ApplicationSubmissionsStat create(
      String programName,
      String localizedProgramName,
      Optional<Timestamp> timestamp,
      long applicationCount,
      double submissionDurationSeconds25p,
      double submissionDurationSeconds50p,
      double submissionDurationSeconds75p,
      double submissionDurationSeconds99p) {
    return new AutoValue_ApplicationSubmissionsStat(
        programName,
        localizedProgramName,
        timestamp,
        applicationCount,
        submissionDurationSeconds25p,
        submissionDurationSeconds50p,
        submissionDurationSeconds75p,
        submissionDurationSeconds99p);
  }

  /** The name of the program the applications were submitted for. */
  public abstract String programName();

  /** The localized name of the program the applications were submitted for. */
  public abstract String localizedProgramName();

  /** A timestamp representing the month they were submitted. */
  public abstract Optional<Timestamp> timestamp();

  /** The number of applications submitted that month. */
  public abstract long applicationCount();

  /** The 25th percentile average of the submission time - creation time. */
  public abstract double submissionDurationSeconds25p();

  /** The 50th percentile average of the submission time - creation time. */
  public abstract double submissionDurationSeconds50p();

  /** The 75th percentile average of the submission time - creation time. */
  public abstract double submissionDurationSeconds75p();

  /** The 99th percentile average of the submission time - creation time. */
  public abstract double submissionDurationSeconds99p();

  static final class Aggregator {
    private final String programName;
    private final String localizedProgramName;
    private final Optional<Timestamp> timestamp;
    private long count = 0;
    private double p25WithWeights = 0;
    private double p50WithWeights = 0;
    private double p75WithWeights = 0;
    private double p99WithWeights = 0;

    Aggregator(String programName, String localizedProgramName, Timestamp timestamp) {
      this.programName = Preconditions.checkNotNull(programName);
      this.localizedProgramName = Preconditions.checkNotNull(localizedProgramName);
      this.timestamp = Optional.of(Preconditions.checkNotNull(timestamp));
    }

    Aggregator(String programName, String localizedProgramName) {
      this.programName = Preconditions.checkNotNull(programName);
      this.localizedProgramName = Preconditions.checkNotNull(localizedProgramName);
      this.timestamp = Optional.empty();
    }

    void update(ApplicationSubmissionsStat stat) {
      count += stat.applicationCount();
      p25WithWeights += stat.submissionDurationSeconds25p() * stat.applicationCount();
      p50WithWeights += stat.submissionDurationSeconds50p() * stat.applicationCount();
      p75WithWeights += stat.submissionDurationSeconds75p() * stat.applicationCount();
      p99WithWeights += stat.submissionDurationSeconds99p() * stat.applicationCount();
    }

    ApplicationSubmissionsStat getAggregateStat() {
      return ApplicationSubmissionsStat.create(
          programName,
          localizedProgramName,
          timestamp,
          count,
          p25WithWeights / count,
          p50WithWeights / count,
          p75WithWeights / count,
          p99WithWeights / count);
    }
  }
}
