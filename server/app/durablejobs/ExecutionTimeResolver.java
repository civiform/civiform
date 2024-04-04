package durablejobs;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public abstract class ExecutionTimeResolver {

  protected final Optional<LocalDate> date;

  @Inject
  public ExecutionTimeResolver(Optional<LocalDate> date) {
    this.date = date;
  }

  /**
   * Computes the next execution time for a recurring job based on the local time settings
   * represented by {@code clock}.
   */
  public abstract Instant resolveExecutionTime(Clock clock);
}
