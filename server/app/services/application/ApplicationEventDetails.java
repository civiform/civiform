package services.application;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;

// A single Application event and its details.
//
// This class is intended to represent the relevant details of some event related to an Application.
// The event is identified by {code EventType} and it's corresponding data; as such only one of the
// Optional detail fields should be present. This class will be json serialized into a database
// column.
@AutoValue
@JsonDeserialize(builder = AutoValue_ApplicationEventDetails.Builder.class)
public abstract class ApplicationEventDetails {
  // The event affecting the application.
  public abstract Type eventType();

  public abstract Optional<StatusEvent> statusEvent();

  public abstract Optional<NoteEvent> noteEvent();

  public static Builder builder() {
    return new AutoValue_ApplicationEventDetails.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setEventType(Type type);

    public abstract Builder setStatusEvent(StatusEvent event);

    public abstract Builder setNoteEvent(NoteEvent event);

    abstract ApplicationEventDetails build();
  }

  // The type of Event being recorded.
  public enum Type {
    STATUS_CHANGE,
    NOTE_CHANGE;
  }

  @AutoValue
  @JsonDeserialize(builder = AutoValue_ApplicationEventDetails_StatusEvent.Builder.class)
  public abstract static class StatusEvent {
    // The text of the StatusDefinitions.Status applied to the application in the default locale.
    public abstract String statusText();
    // If the status's email was sent.
    public abstract Boolean emailSent();

     public static Builder builder() {
      return new AutoValue_ApplicationEventDetails_StatusEvent.Builder();
     }

     @AutoValue.Builder
     public abstract static class Builder {
      public abstract Builder setStatusText(String statusText);
      public abstract Builder setEmailSent(Boolean emailSent);
      public abstract StatusEvent build();
     }
  }

  @AutoValue
  public abstract static class NoteEvent {
    static NoteEvent create(String note) {
      return new AutoValue_ApplicationEventDetails_NoteEvent(note);
    }
    // A note set on the application.
    abstract String note();
  }
}
