package services.ti;

public final class DateOfBirthNotInPastException extends Exception {
  public DateOfBirthNotInPastException(String message) {
    super(message);
  }
}
