package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import play.i18n.Messages;
import services.MessageKey;

/**
 * This is a wrapper around Play's {@link Messages}, for use when we don't have access to messages
 * yet.
 */
@AutoValue
public abstract class ValidationErrorMessage {

  public static ValidationErrorMessage create(MessageKey key, Object... args) {
    return new AutoValue_ValidationErrorMessage(key, ImmutableList.copyOf(args));
  }

  public abstract MessageKey key();

  public abstract ImmutableList<Object> args();

  public String getMessage(Messages messages) {
    return key().getMessage(messages, args().toArray());
  }
}
