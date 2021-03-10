package auth;

import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;

public class ProfileMergeConflictException extends HttpAction implements WithContentAction {
  private final String message;

  ProfileMergeConflictException(String message) {
    super(400);
    this.message = message;
  }

  @Override
  public String getContent() {
    return String.format("Failed to merge your existing profile with the new one: \"%s\"", message);
  }
}
