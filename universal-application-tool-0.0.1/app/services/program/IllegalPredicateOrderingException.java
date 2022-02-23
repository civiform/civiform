package services.program;

/**
 * IllegalPredicateOrderingException is thrown when a screen (block) is moved to a position that
 * breaks existing predicate configuration.
 */
public class IllegalPredicateOrderingException extends Exception {

  public IllegalPredicateOrderingException(String message) {
    super(message);
  }
}
