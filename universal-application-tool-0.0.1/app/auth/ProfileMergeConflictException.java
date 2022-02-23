package auth;

import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;

public class ProfileMergeConflictException extends HttpAction implements WithContentAction {
  private final String message;

  /**
   * When this exception is thrown in a controller, the resulting response will be a 400 and the
   * provided message will be displayed to the user (unformatted, for now).
   */
  ProfileMergeConflictException(String message) {
    super(400);
    this.message = message;
  }

  @Override
  public String getContent() {
    return String.format("Failed to merge your existing profile with the new one: \"%s\"", message);
  }
}
