package services.applications;

/* Exception for when an Account email doesn't exist but is used. */
public final class AccountHasNoEmailException extends Exception {
  public AccountHasNoEmailException(Long accountId) {
    super(
        String.format(
            "An email address was requested for account id %d, but it does not have one.",
            accountId));
  }
}
