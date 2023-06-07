package services.applicant;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;

/**
 * Represents a human-readable label for an applicant, such as the one that is displayed on the
 * header of CiviForm.
 */
@AutoOneOf(ApplicantPersonalInfo.ApplicantType.class)
public abstract class ApplicantPersonalInfo {
  public enum ApplicantType {
    LOGGED_IN,
    GUEST
  }

  public abstract ApplicantType getType();

  public String getDisplayString(Messages messages) {
    switch (getType()) {
      case GUEST:
        return messages.at(MessageKey.GUEST.getKeyName());

      case LOGGED_IN:
        LoggedInRepresentation loggedIn = loggedIn();
        if (loggedIn.name().isPresent()) {
          return loggedIn.name().get();
        } else if (loggedIn.email().isPresent()) {
          return loggedIn.email().get();
        }

        // Fall through
      default:
        throw new RuntimeException(
            "ApplicantPersonalInfo has no name or email and is not a guest.");
    }
  }

  public abstract void guest();

  public abstract LoggedInRepresentation loggedIn();

  public static ApplicantPersonalInfo ofGuestUser() {
    return AutoOneOf_ApplicantPersonalInfo.guest();
  }

  public static ApplicantPersonalInfo ofLoggedInUser(
      LoggedInRepresentation loggedInRepresentation) {
    return AutoOneOf_ApplicantPersonalInfo.loggedIn(loggedInRepresentation);
  }

  @AutoValue
  public abstract static class LoggedInRepresentation {
    public abstract Optional<String> name();

    public abstract Optional<String> email();

    public static Builder builder() {
      return new AutoValue_ApplicantPersonalInfo_LoggedInRepresentation.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setName(String name);

      public abstract Builder setName(Optional<String> name);

      public abstract Builder setEmail(String email);

      public abstract LoggedInRepresentation build();
    }
  }
}
