package services.openApi.v2;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * Contact information for the exposed API.
 *
 * <p>https://swagger.io/specification/v2/#contact-object
 */
@AutoValue
public abstract class Contact {
  /** The identifying name of the contact person/organization. */
  public abstract Optional<String> getName();

  /**
   * The email address of the contact person/organization. MUST be in the format of an email
   * address.
   */
  public abstract Optional<String> getEmail();

  public static Contact.Builder builder() {
    return new AutoValue_Contact.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Contact.Builder setName(String name);

    public abstract Contact.Builder setEmail(String email);

    public abstract Contact build();
  }
}
