package services.program;

import com.google.auto.value.AutoValue;

/** Represents a failure for a program service operation. */
@AutoValue
public abstract class ProgramServiceError {

  public static ProgramServiceError of(String message) {
    return new AutoValue_ProgramServiceError(message);
  }

  /** The failure message. */
  public abstract String message();
}
