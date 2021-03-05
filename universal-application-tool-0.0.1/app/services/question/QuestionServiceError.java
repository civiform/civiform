package services.question;

import com.google.auto.value.AutoValue;

/** Represents a failure for a question service operation. */
@AutoValue
public abstract class QuestionServiceError {

  public static QuestionServiceError of(String message) {
    return new AutoValue_QuestionServiceError(message);
  }

  /** The failure message. */
  public abstract String message();
}
