package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.Test;
import repository.ResetPostgres;

public class PersistedDurableJobTest extends ResetPostgres {

  @Test
  public void canBePersisted() {
    var job = new PersistedDurableJob("fake-job-name", Instant.ofEpochMilli(1000));

    job.save();
    assertThat(job.id).isNotNull();
  }

  @Test
  public void appendingErrors() {
    var job = new PersistedDurableJob("fake-job-name", Instant.ofEpochMilli(1000));

    assertThat(job.getErrorMessage()).isEmpty();
    job.appendErrorMessage("first message");
    assertThat(job.getErrorMessage().get()).isEqualTo("first message");
    job.appendErrorMessage("second message");
    assertThat(job.getErrorMessage().get()).isEqualTo("first message\nEND_ERROR\n\nsecond message");
  }

  @Test
  public void decrementRemainingAttempts() {
    var job = new PersistedDurableJob("fake-job-name", Instant.ofEpochMilli(1000));

    assertThat(job.getRemainingAttempts()).isEqualTo(3);
    job.decrementRemainingAttempts();
    assertThat(job.getRemainingAttempts()).isEqualTo(2);
  }
}
