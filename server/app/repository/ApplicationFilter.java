package repository;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;

@AutoValue
public abstract class ApplicationFilter {
  public abstract Optional<TimeFilter> submitTimeFilter();

  public static Builder builder() {
    return new AutoValue_ApplicationFilter.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSubmitTimeFilter(Optional<TimeFilter> v);

    public abstract ApplicationFilter build();
  }

  @AutoValue
  public abstract static class TimeFilter {
    public abstract Optional<Instant> beforeTime();

    public abstract Optional<Instant> afterTime();

    public static Builder builder() {
      return new AutoValue_ApplicationFilter_TimeFilter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setBeforeTime(Optional<Instant> v);

      public abstract Builder setAfterTime(Optional<Instant> v);

      public abstract TimeFilter build();
    }
  }
}
