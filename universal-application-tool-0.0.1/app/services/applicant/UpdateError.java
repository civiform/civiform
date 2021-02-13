package services.applicant;

import com.google.auto.value.AutoValue;

/** Represents a single validation failure for a scalar update. */
@AutoValue
public abstract class UpdateError {

  /** The path to the scalar for the attempted update. */
  abstract String path();

  /** The validation failure message. */
  abstract String message();
}
