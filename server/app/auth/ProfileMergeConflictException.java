package auth;

import org.pac4j.core.exception.TechnicalException;

public class ProfileMergeConflictException extends TechnicalException {

  /**
   * When this exception is thrown in a controller, the resulting response will be a 400 and the
   * provided content will be displayed to the user (unformatted, for now).
   */
  ProfileMergeConflictException(String content) {
    super(content);
  }
}
