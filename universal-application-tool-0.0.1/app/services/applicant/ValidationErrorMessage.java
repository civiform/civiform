package services.applicant;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ValidationErrorMessage {

  // TODO: move this to language specific files so we can support multiple locales.
  public static String TEXT_TOO_SHORT = "Must contain at least %d characters.";
  public static String TEXT_TOO_LONG = "Must contain at most %d characters.";
  public static String NUMBER_TOO_SMALL = "Must be at least %d.";
  public static String NUMBER_TOO_BIG = "Must be at most %d.";
  public static String TOO_FEW_SELECTIONS = "Please select at least %d.";
  public static String TOO_MANY_SELECTIONS = "Please select fewer than %d.";

  public abstract String message();

  // TODO: Ability to get message for a given locale. Probably requires taking in a map from locale
  //  to message.
  public static ValidationErrorMessage create(String message) {
    return new AutoValue_ValidationErrorMessage(message);
  }

  public static ValidationErrorMessage textTooShortError(int minLength) {
    return create(String.format(TEXT_TOO_SHORT, minLength));
  }

  public static ValidationErrorMessage textTooLongError(int maxLength) {
    return create(String.format(TEXT_TOO_LONG, maxLength));
  }

  public static ValidationErrorMessage numberTooSmallError(long min) {
    return create(String.format(NUMBER_TOO_SMALL, min));
  }

  public static ValidationErrorMessage numberTooLargeError(long max) {
    return create(String.format(NUMBER_TOO_BIG, max));
  }

  public static ValidationErrorMessage tooFewSelectionsError(int min) {
    return create(String.format(TOO_FEW_SELECTIONS, min));
  }

  public static ValidationErrorMessage tooManySelectionsError(int max) {
    return create(String.format(TOO_MANY_SELECTIONS, max));
  }
}
