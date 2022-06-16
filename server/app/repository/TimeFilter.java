package repository;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;

/** Allows filtering data based on an optional fromTime (inclusive) / toTime (exclusive).ß */
@AutoValue
public abstract class TimeFilter {
  public abstract Optional<Instant> fromTime();

  public abstract Optional<Instant> toTime();

  public static Builder builder() {
    return new AutoValue_TimeFilter.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setFromTime(Optional<Instant> v);

    public abstract Builder setToTime(Optional<Instant> v);

    public abstract TimeFilter build();
  }
}
