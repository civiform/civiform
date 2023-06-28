package services.applicant;

import static services.applicant.ApplicantPersonalInfo.ApplicantType.LOGGED_IN;

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
    // A canonical logged-in user. A user is logged in if and only if they have an authority ID.
    LOGGED_IN,
    // When a TI creates a user, they are assigned an email but not an authority ID - that gets
    // assigned when the user logs in themselves. For display pursposes, we need to make the
    // distinction between guest users without an authority ID and TI-created users that don't yet
    // have an authority ID.
    TI_PARTIALLY_CREATED,
    // A guest does not have an authority ID, and they were not created by a TI.
    GUEST
  }

  public abstract ApplicantType getType();

  public String getDisplayString(Messages messages) {
    switch (getType()) {
      case LOGGED_IN:
      case TI_PARTIALLY_CREATED:
        Representation representation = getType() == LOGGED_IN ? loggedIn() : tiPartiallyCreated();

        if (representation.name().isPresent()) {
          return representation.name().get();
        } else if (representation.email().isPresent()) {
          return representation.email().get();
        }

        // Fall through
      case GUEST:
        // Fall through
      default:
        // This case happens with our fake admin account used for debugging.
        return messages.at(MessageKey.GUEST.getKeyName());
    }
  }

  public abstract void guest();

  public abstract Representation loggedIn();

  public abstract Representation tiPartiallyCreated();

  public static ApplicantPersonalInfo ofGuestUser() {
    return AutoOneOf_ApplicantPersonalInfo.guest();
  }

  public static ApplicantPersonalInfo ofLoggedInUser(Representation representation) {
    return AutoOneOf_ApplicantPersonalInfo.loggedIn(representation);
  }

  public static ApplicantPersonalInfo ofTiPartiallyCreated(Representation representation) {
    return AutoOneOf_ApplicantPersonalInfo.tiPartiallyCreated(representation);
  }

  @AutoValue
  public abstract static class Representation {
    public abstract Optional<String> name();

    public abstract Optional<String> email();

    public static Builder builder() {
      return new AutoValue_ApplicantPersonalInfo_Representation.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setName(String name);

      public abstract Builder setName(Optional<String> name);

      public abstract Builder setEmail(String email);

      public abstract Representation build();
    }
  }
}
