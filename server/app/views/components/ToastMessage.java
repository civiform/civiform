package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.specialized.DivTag;
import java.util.UUID;
import play.i18n.Messages;
import services.MessageKey;
import views.style.ReferenceClasses;

/** ToastMessages are messages that appear on the screen to show information to the user. */
public final class ToastMessage {

  public enum ToastType {
    ALERT,
    ERROR,
    SUCCESS,
    WARNING
  }

  private ToastType type;

  /** Toast messages are instantiated with a random id. */
  private String id = UUID.randomUUID().toString();

  private String message;

  /** Default duration is to not auto-hide. */
  private int duration = 0;

  private boolean canDismiss = true;

  /** If true this message will not be shown if a user has already seen and dismissed it. */
  private boolean canIgnore = false;

  private String condOnStorageKey;

  private ToastMessage(String message, ToastType severity) {
    this.message = checkNotNull(message);
    this.type = checkNotNull(severity);
    this.setDismissible(!ToastType.ERROR.equals(severity));
  }

  public static ToastMessage alert(String message) {
    return new ToastMessage(message, ToastType.ALERT);
  }

  public static ToastMessage error(String message, Messages messages) {
    String errMessageWithPrefix =
        messages.at(MessageKey.TOAST_ERROR_MSG_OUTLINE.getKeyName(), message);
    return new ToastMessage(errMessageWithPrefix, ToastType.ERROR);
  }

  public static ToastMessage errorNonLocalized(String message) {
    return new ToastMessage("Error: " + message, ToastType.ERROR);
  }

  public static ToastMessage success(String message) {
    return new ToastMessage(message, ToastType.SUCCESS);
  }

  public static ToastMessage warning(String message) {
    return new ToastMessage(message, ToastType.WARNING);
  }

  /** If true then a dismiss button will be visible for this toast. */
  public ToastMessage setDismissible(boolean canDismiss) {
    this.canDismiss = canDismiss;
    return this;
  }

  /**
   * If true, dismissing the toast message will prevent other toast messages with the same id from
   * being displayed on subsequent pages.
   */
  public ToastMessage setIgnorable(boolean canIgnore) {
    this.canIgnore = canIgnore;
    return this;
  }

  /**
   * How long, in ms, the toast displays before auto-hiding. A duration <= 0 indicates that the
   * toast is never automatically hidden.
   */
  public ToastMessage setDuration(int duration) {
    this.duration = duration;
    return this;
  }

  public ToastMessage setId(String id) {
    this.id = id;
    return this;
  }

  public ToastMessage setMessage(String message) {
    this.message = message;
    return this;
  }

  public ToastMessage setType(ToastType type) {
    this.type = type;
    return this;
  }

  public ToastMessage setCondOnStorageKey(String storageKey) {
    this.condOnStorageKey = storageKey;
    return this;
  }

  public DivTag getContainerTag() {
    DivTag ret =
        div(this.message)
            .withClasses("hidden", ReferenceClasses.TOAST_MESSAGE)
            .withId(this.id)
            .attr("canDismiss", this.canDismiss)
            .attr("canIgnore", this.canIgnore)
            .attr("toastDuration", this.duration)
            .attr("condOnStorageKey", this.condOnStorageKey)
            .attr("toastType", this.type);
    return ret;
  }
}
