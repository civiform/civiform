package repository;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.LifecycleStage;

/**
 * Filters that can be applied to retrieve a list of submitted applications matching all of the
 * provided data. This does not include draft or deleted applications.
 */
@AutoValue
public abstract class SubmittedApplicationFilter {
  public static final String NO_STATUS_FILTERS_OPTION_UUID = "ad80b347-5ae4-43c3-8578-e503b043be12";

  public static final SubmittedApplicationFilter EMPTY =
      SubmittedApplicationFilter.builder()
          .setSubmitTimeFilter(TimeFilter.EMPTY)
          // By default, retrieves all submitted applications.
          .setLifecycleStages(ImmutableList.of(LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
          .build();

  /**
   * If provided and is an unsigned integer, the query will filter to applications with an applicant
   * ID matching it. If provided and is not an unsigned integer, the query will filter to
   * applications with email, first name, or last name that contain it.
   */
  public abstract Optional<String> searchNameFragment();

  /** Filters to applications that were submitted within the provided date range. */
  public abstract TimeFilter submitTimeFilter();

  /**
   * If specified and non-empty, returns applications that match the provided status. A value of
   * NO_STATUS_FILTERS_OPTION_UUID will match only applications with no status explicitly set.
   */
  public abstract Optional<String> applicationStatus();

  /** Filters to applications with the specified lifecycle stages. */
  public abstract ImmutableList<LifecycleStage> lifecycleStages();

  public static Builder builder() {
    return new AutoValue_SubmittedApplicationFilter.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSearchNameFragment(Optional<String> v);

    public abstract Builder setSubmitTimeFilter(TimeFilter v);

    public abstract Builder setApplicationStatus(Optional<String> v);

    public abstract Builder setLifecycleStages(ImmutableList<LifecycleStage> v);

    public abstract SubmittedApplicationFilter build();
  }
}
