package repository;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;

/** Allows filtering data based on an optional fromTime (inclusive) / untilTime (exclusive). */
@AutoValue
public abstract class TimeFilter {
  public static final TimeFilter EMPTY = TimeFilter.builder().build();

  public abstract Optional<Instant> fromTime();

  public abstract Optional<Instant> untilTime();

  public static Builder builder() {
    return new AutoValue_TimeFilter.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setFromTime(Optional<Instant> v);

    public abstract Builder setUntilTime(Optional<Instant> v);

    public abstract TimeFilter build();
  }
}
