package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import play.i18n.Messages;
import services.MessageKey;

/**
 * This acts a wrapper around Play's {@link Messages}, for use when we don't have access to messages
 * yet. We typically only have access to {@link Messages} in renderers, but validation error
 * generation happens in the applicant service. Therefore, we use this class to collect the
 * information needed to generate validation error messages in the applicant's preferred language
 * later in the view classes.
 */
@AutoValue
public abstract class ValidationErrorMessage {

  public static ValidationErrorMessage create(MessageKey key, Object... args) {
    return new AutoValue_ValidationErrorMessage(key, ImmutableList.copyOf(args));
  }

  public abstract MessageKey key();

  public abstract ImmutableList<Object> args();

  public boolean isRequiredError() {
    return key().equals(MessageKey.VALIDATION_REQUIRED);
  }

  public String getMessage(Messages messages) {
    return messages.apply(
        MessageKey.TOAST_ERROR_MSG_OUTLINE.getKeyName(),
        messages.at(key().getKeyName(), args().toArray()));
  }
}
