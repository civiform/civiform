package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * Captures an update to the localized text associated with a program and its associated application
 * statuses.
 */
@AutoValue
public abstract class LocalizationUpdate {

  public abstract String localizedDisplayName();

  public abstract String localizedDisplayDescription();

  public abstract String localizedConfirmationMessage();

  public abstract Optional<String> localizedSummaryImageDescription();

  public abstract ImmutableList<StatusUpdate> statuses();

  public static Builder builder() {
    return new AutoValue_LocalizationUpdate.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLocalizedDisplayName(String v);

    public abstract Builder setLocalizedDisplayDescription(String v);

    public abstract Builder setLocalizedConfirmationMessage(String v);

    public abstract Builder setLocalizedSummaryImageDescription(String v);

    public abstract Builder setStatuses(ImmutableList<StatusUpdate> v);

    public abstract LocalizationUpdate build();
  }

  /**
   * Captures updates to the translations for a given program status, identified by
   * configuredStatusText.
   */
  @AutoValue
  public abstract static class StatusUpdate {
    /**
     * The non-localized status text that is configured for the program. This identifies which
     * entry's localized content should be updated.
     */
    public abstract String statusKeyToUpdate();

    /** The new status text to update for a locale. */
    public abstract Optional<String> localizedStatusText();

    /** The new email body to update for a locale. */
    public abstract Optional<String> localizedEmailBody();

    public static Builder builder() {
      return new AutoValue_LocalizationUpdate_StatusUpdate.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setStatusKeyToUpdate(String v);

      public abstract Builder setLocalizedStatusText(Optional<String> v);

      public abstract Builder setLocalizedEmailBody(Optional<String> v);

      public abstract StatusUpdate build();
    }
  }
}
