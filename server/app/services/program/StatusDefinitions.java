package services.program;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.LocalizedStrings;

/** Contains data defining status tracking configuration for a program's applications. */
public class StatusDefinitions {

  // The configured status options in their natural ordering.
  @JsonProperty("statuses")
  private ImmutableList<Status> statuses;

  @JsonCreator
  public StatusDefinitions(@JsonProperty("statuses") ImmutableList<Status> statuses) {
    assertStatusNamesUnique(statuses);
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

  /**
   * Sets {@code statuses} as the configured {@link Status} values.
   *
   * <p>The order of the items will be maintained and used as the natural order of the statuses.
   */
  public void setStatuses(ImmutableList<Status> statuses) {
    assertStatusNamesUnique(statuses);
    this.statuses = statuses;
  }

  private static void assertStatusNamesUnique(ImmutableList<Status> statuses) {
    Preconditions.checkState(
        statuses.stream().map(Status::statusText).collect(ImmutableSet.toImmutableSet()).size()
            == statuses.size(),
        "The provided set of statuses must have unique statusTexts.");
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

    @JsonProperty("email_body")
    public abstract Optional<String> emailBodyText();

    @JsonProperty("email_body_localized")
    public abstract Optional<LocalizedStrings> localizedEmailBodyText();

    public static Builder builder() {
      return new AutoValue_StatusDefinitions_Status.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("status")
      public abstract Builder setStatusText(String value);

      @JsonProperty("status_localized")
      public abstract Builder setLocalizedStatusText(LocalizedStrings value);

      @JsonProperty("email_body")
      public abstract Builder setEmailBodyText(String value);

      @JsonProperty("email_body_localized")
      public abstract Builder setLocalizedEmailBodyText(Optional<LocalizedStrings> value);

      public abstract Status build();
    }
  }
}
