package models;

import com.google.common.base.Preconditions;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Table;
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
public final class PersistedDurableJob extends BaseModel {

  @Constraints.Required private String jobName;
  @Constraints.Required private Instant executionTime;

  private Instant successTime;

  @WhenCreated private Instant createTime;

  @Constraints.Required private int remainingAttempts;

  private String errorMessage;

  public PersistedDurableJob(String jobName, Instant executionTime) {
    this.jobName = Preconditions.checkNotNull(jobName);
    this.executionTime = Preconditions.checkNotNull(executionTime);
    this.remainingAttempts = 3;
  }

  public String getJobName() {
    return jobName;
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

  public PersistedDurableJob setSuccessTime(Instant time) {
    this.successTime = time;
    return this;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public int getRemainingAttempts() {
    return remainingAttempts;
  }

  public PersistedDurableJob decrementRemainingAttempts() {
    this.remainingAttempts--;
    return this;
  }

  public Optional<String> getErrorMessage() {
    return Optional.ofNullable(errorMessage);
  }

  public PersistedDurableJob appendErrorMessage(String newMessage) {
    this.errorMessage =
        this.errorMessage == null
            ? newMessage
            : String.format("%s\nEND_ERROR\n\n%s", this.errorMessage, newMessage);
    return this;
  }
}
