package services.ti;

public class DateOfBirthNotInPastException extends Exception {
  public DateOfBirthNotInPastException(String message) {
    super(message);
  }
}
