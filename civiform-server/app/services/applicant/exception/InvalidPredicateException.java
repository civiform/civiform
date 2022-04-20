package services.applicant.exception;

/**
 * InvalidPredicateException is thrown when a screen (block) visibility predicate is misconfigured.
 */
public class InvalidPredicateException extends Exception {

  public InvalidPredicateException(String message) {
    super(message);
  }
}
