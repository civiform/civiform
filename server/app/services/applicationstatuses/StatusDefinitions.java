package services.applicationstatuses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.LocalizedStrings;

/** Contains data defining status tracking configuration for a program's applications. */
public final class StatusDefinitions {

  // The configured status options in their natural ordering.
  @JsonProperty("statuses")
  private ImmutableList<Status> statuses;

  @JsonCreator
  public StatusDefinitions(@JsonProperty("statuses") ImmutableList<Status> statuses) {
    assertStatusNamesNonEmptyAndUnique(statuses);
    this.statuses = statuses;
  }

  /** Constructs a {@code StatusDefinitions} with no {@link Status} values. */
  public StatusDefinitions() {
    statuses = ImmutableList.of();
  }

  /** Returns the {@link Status} values in the order originally provided. */
  public ImmutableList<Status> getStatuses() {
    return statuses;
  }

  @Override
  public String toString() {
    return statuses.toString();
  }

  /**
   * Sets {@code statuses} as the configured {@link Status} values.
   *
   * <p>The order of the items will be maintained and used as the natural order of the statuses.
   *
   * @return this, for chaining.
   */
  public StatusDefinitions setStatuses(ImmutableList<Status> statuses) {
    assertStatusNamesNonEmptyAndUnique(statuses);
    this.statuses = statuses;
    return this;
  }

  private static void assertStatusNamesNonEmptyAndUnique(ImmutableList<Status> statuses) {
    Preconditions.checkState(
        statuses.stream().map(Status::statusText).distinct().count() == statuses.size(),
        "The provided set of statuses must have unique statusTexts.");
    Preconditions.checkState(
        statuses.stream().map(Status::statusText).noneMatch(String::isEmpty),
        "The provided set of statuses may not contain empty statusTexts.");
  }

  @JsonIgnore
  public Optional<StatusDefinitions.Status> getDefaultStatus() {
    return statuses.stream().filter(StatusDefinitions.Status::computedDefaultStatus).findFirst();
  }

  /**
   * Defines a single status.
   *
   * <p>Email body is optionally defined and both status and email support localization.
   */
  @AutoValue
  @JsonDeserialize(builder = AutoValue_StatusDefinitions_Status.Builder.class)
  public abstract static class Status {

    @JsonProperty("status")
    public abstract String statusText();

    @JsonProperty("status_localized")
    public abstract LocalizedStrings localizedStatusText();

    @JsonProperty("email_body_localized")
    public abstract Optional<LocalizedStrings> localizedEmailBodyText();

    @JsonProperty("defaultStatus")
    public abstract Optional<Boolean> defaultStatus();

    // Because statuses created before this feature was released will not
    // have a defaultStatus field, use this to check if the status is set
    // as the default status.
    public boolean computedDefaultStatus() {
      return defaultStatus().orElse(false);
    }

    public boolean matches(String otherStatusString) {
      return statusText().equals(otherStatusString);
    }

    public static Builder builder() {
      return new AutoValue_StatusDefinitions_Status.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("status")
      public abstract Builder setStatusText(String value);

      @JsonProperty("status_localized")
      public abstract Builder setLocalizedStatusText(LocalizedStrings value);

      @JsonProperty("email_body_localized")
      public abstract Builder setLocalizedEmailBodyText(Optional<LocalizedStrings> value);

      @JsonProperty("defaultStatus")
      public abstract Builder setDefaultStatus(Optional<Boolean> value);

      public abstract Status build();
    }
  }
}
