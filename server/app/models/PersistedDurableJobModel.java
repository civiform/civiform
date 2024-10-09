package models;

import com.google.common.base.Preconditions;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import play.data.validation.Constraints;

/**
 * An EBean mapped class that records a durable job execution in the database.
 *
 * <p>Each {@code PersistedDurableJob} has a {@code jobName}, that determines which job should
 * execute when its {@code executionTime} is reached.
 *
 * <p>When jobs fail, a failure message is recorded on the {@code PersistedDurableJob}'s {@code
 * errorMessage} and its {@code remainingAttempts} count is decremented.
 */
@Entity
@Table(name = "persisted_durable_jobs")
public final class PersistedDurableJobModel extends BaseModel {

  @Constraints.Required private String jobName;
  @Constraints.Required private JobType jobType;
  @Constraints.Required private Instant executionTime;

  private Instant successTime;

  @WhenCreated private Instant createTime;

  @Constraints.Required private int remainingAttempts;

  private String errorMessage;

  public PersistedDurableJobModel(String jobName, JobType jobType, Instant executionTime) {
    this.jobName = Preconditions.checkNotNull(jobName);
    this.jobType = jobType;
    this.executionTime = Preconditions.checkNotNull(executionTime);
    this.remainingAttempts = 3;
  }

  public String getJobName() {
    return jobName;
  }

  public JobType getJobType() {
    return jobType;
  }

  public Instant getExecutionTime() {
    return executionTime;
  }

  public boolean hasFailedWithNoRemainingAttempts() {
    return successTime == null && remainingAttempts == 0;
  }

  public Optional<Instant> getSuccessTime() {
    return Optional.ofNullable(successTime);
  }

  public PersistedDurableJobModel setSuccessTime(Instant time) {
    this.successTime = time;
    return this;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public int getRemainingAttempts() {
    return remainingAttempts;
  }

  public PersistedDurableJobModel decrementRemainingAttempts() {
    this.remainingAttempts--;
    return this;
  }

  public Optional<String> getErrorMessage() {
    return Optional.ofNullable(errorMessage);
  }

  public PersistedDurableJobModel appendErrorMessage(String newMessage) {
    this.errorMessage =
        this.errorMessage == null
            ? newMessage
            : String.format("%s\nEND_ERROR\n\n%s", this.errorMessage, newMessage);
    return this;
  }
}
