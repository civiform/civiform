package services.applicant;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ValidationErrorMessage {

  public abstract String message();

  // TODO: Ability to get message for a given locale. Probably requires taking in a map from locale
  // to message.
  public static ValidationErrorMessage create(String message) {
    return new AutoValue_ValidationErrorMessage(message);
  }
}
