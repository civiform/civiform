package services;

import com.google.auto.value.AutoValue;

/** Represents a failure for a civiform service operation. */
@AutoValue
public abstract class CiviFormError {
  public static CiviFormError of(String message) {
    return new AutoValue_CiviFormError(message);
  }

  /** The failure message. */
  public abstract String message();
}
