package auth;

import java.util.NoSuchElementException;

/** AccountNonexistentException is raised when an account cannot be found. */
public class AccountNonexistentException extends NoSuchElementException {
  public AccountNonexistentException(String message) {
    super(message);
  }
}
