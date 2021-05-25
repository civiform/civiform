package auth;

import java.util.NoSuchElementException;

public class AccountNonexistentException extends NoSuchElementException {
  AccountNonexistentException(String message) {
    super(message);
  }
}
