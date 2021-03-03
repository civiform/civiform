package services.question;

import com.google.auto.value.AutoValue;

/** Represents a failure for a question service operation. */
@AutoValue
public abstract class QuestionServiceError {

  /** The failure message. */
  abstract String message();
}
