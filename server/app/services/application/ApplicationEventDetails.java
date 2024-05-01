package services.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * The details of a single Application event based on its type.
 *
 * <p>This class is intended to represent the specific relevant details of some event related to an
 * {@code Application}. The event is identified by {code EventType} and its corresponding data; as
 * such only one of the Optional detail fields should be present. This class will be json serialized
 * into a database column.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_ApplicationEventDetails.Builder.class)
public abstract class ApplicationEventDetails {
  /** The event affecting the application. */
  @JsonProperty("event_type")
  public abstract Type eventType();

  // Only one of the following Event fields should be set.
  // The JsonInclude suppresses empty Optionals from being serialized.
  /** Type STATUS_EVENT representing a change in the Status Tracking status value. */
  @JsonInclude(Include.NON_EMPTY)
  @JsonProperty("status_event")
  public abstract Optional<StatusEvent> statusEvent();

  /** Type NOTE_EVENT representing a note associated with the Application. */
  @JsonInclude(Include.NON_EMPTY)
  @JsonProperty("note_event")
  public abstract Optional<NoteEvent> noteEvent();

  public static Builder builder() {
    return new AutoValue_ApplicationEventDetails.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("event_type")
    public abstract Builder setEventType(Type type);

    @JsonProperty("note_event")
    public abstract Builder setNoteEvent(NoteEvent event);

    @JsonProperty("status_event")
    public abstract Builder setStatusEvent(StatusEvent event);

    abstract ApplicationEventDetails autoBuild();

    public final ApplicationEventDetails build() {
      ApplicationEventDetails details = autoBuild();
      // XOR check that one and only one optional is set.
      Preconditions.checkArgument(
          details.statusEvent().isPresent() ^ details.noteEvent().isPresent(),
          "One and only one detail in ApplicationEventDetail must be set.");
      return details;
    }
  }

  // The type of Event being recorded.
  public enum Type {
    // A note was set.
    NOTE_CHANGE,
    // The Status Tracking status was changed.
    STATUS_CHANGE
  }

  @AutoValue
  @JsonDeserialize(builder = AutoValue_ApplicationEventDetails_StatusEvent.Builder.class)
  public abstract static class StatusEvent {
    /**
     * The text of the StatusDefinitions.Status applied to the application in the default locale.
     */
    @JsonProperty("status_text")
    public abstract String statusText();

    /** If the status has email content and if it was sent as part of setting the status. */
    @JsonProperty("email_sent")
    public abstract Boolean emailSent();

    public static Builder builder() {
      return new AutoValue_ApplicationEventDetails_StatusEvent.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      @JsonProperty("status_text")
      public abstract Builder setStatusText(String statusText);

      @JsonProperty("email_sent")
      public abstract Builder setEmailSent(Boolean emailSent);

      public abstract StatusEvent build();
    }
  }

  @AutoValue
  public abstract static class NoteEvent {
    @JsonCreator
    public static NoteEvent create(@JsonProperty("note") String note) {
      return new AutoValue_ApplicationEventDetails_NoteEvent(note);
    }

    /** A note set on the application. */
    @JsonProperty("note")
    public abstract String note();
  }
}
