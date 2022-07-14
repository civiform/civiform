package services.program;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.LocalizedStrings;

public class StatusDefinitions {

  @JsonProperty("statuses")
  private ImmutableList<Status> statuses;

  @JsonCreator
  public StatusDefinitions(@JsonProperty("statuses") ImmutableList<Status> statuses) {
    this.statuses = ImmutableList.copyOf(statuses);
  }

  public StatusDefinitions() {
    statuses = ImmutableList.of();
  }

  public ImmutableList<Status> getStatuses() {
    return statuses;
  }

  public void setStatuses(ImmutableList<Status> statuses) {
    this.statuses = statuses;
  }

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
      public abstract Builder setLocalizedEmailBodyText(LocalizedStrings value);

      public abstract Status build();
    }
  }
}
