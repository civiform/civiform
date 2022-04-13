package views.components;

import static j2html.TagCreator.div;

import j2html.tags.specialized.DivTag;

import java.util.UUID;
import views.style.ReferenceClasses;
import views.style.Styles;

/** ToastMessages are messages that appear on the screen to show information to the user. */
public class ToastMessage {

  enum ToastType {
    ALERT,
    ERROR,
    SUCCESS,
    WARNING
  }

  private ToastType type = ToastType.ALERT;

  /** Toast messages are instantiated with a random id. */
  private String id = UUID.randomUUID().toString();

  private String message = "";

  /** Default duration is 3 seconds. */
  private int duration = 3000;

  private boolean canDismiss = true;

  /** If true this message will not be shown if a user has already seen and dismissed it. */
  private boolean canIgnore = false;

  public static ToastMessage alert(String message) {
    return new ToastMessage().setType(ToastType.ALERT).setMessage(message);
  }

  public static ToastMessage error(String message) {
    return new ToastMessage().setType(ToastType.ERROR).setMessage(message);
  }

  public static ToastMessage success(String message) {
    return new ToastMessage().setType(ToastType.SUCCESS).setMessage(message);
  }

  public static ToastMessage warning(String message) {
    return new ToastMessage().setType(ToastType.WARNING).setMessage(message);
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
   * How long the toast displays before auto-hiding. A duration <= 0 indicates that the toast is
   * never automatically hidden.
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

  public DivTag getContainerTag() {
    ContainerTag ret =
        div(this.message)
            .withClasses(Styles.HIDDEN, ReferenceClasses.TOAST_MESSAGE)
            .withId(this.id)
            .attr("canDismiss", this.canDismiss)
            .attr("canIgnore", this.canIgnore)
            .attr("toastDuration", this.duration)
            .attr("toastType", this.type);
    return ret;
  }
}
