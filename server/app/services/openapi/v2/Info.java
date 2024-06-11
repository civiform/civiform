package services.openapi.v2;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * The object provides metadata about the API. The metadata can be used by the clients if needed,
 * and can be presented in the Swagger-UI for convenience.
 *
 * <p>https://swagger.io/specification/v2/#info-object
 */
@AutoValue
public abstract class Info {
  /**
   * Required.
   *
   * <p>The title of the application.
   */
  public abstract String getTitle();

  /**
   * Required.
   *
   * <p>Provides the version of the application API (not to be confused with the specification
   * version).
   */
  public abstract String getVersion();

  /**
   * A short description of the application. GFM syntax can be used for rich text representation.
   */
  public abstract Optional<String> getDescription();

  /** The contact information for the exposed API. */
  public abstract Optional<Contact> getContact();

  public static Info.Builder builder(String title, String version) {
    return new AutoValue_Info.Builder().setTitle(title).setVersion(version);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract Info.Builder setTitle(String title);

    protected abstract Info.Builder setVersion(String version);

    public abstract Info.Builder setDescription(String description);

    public abstract Info.Builder setContact(Contact contact);

    public abstract Info build();
  }
}
