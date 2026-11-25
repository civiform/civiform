package auth;

import java.util.NoSuchElementException;

/** AccountNonexistentException is raised when an account cannot be found. */
public final class AccountNonexistentException extends NoSuchElementException {
  public AccountNonexistentException(String message) {
    super(message);
  }
}
