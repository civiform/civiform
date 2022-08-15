package controllers;

import views.components.ToastMessage.ToastType;

/** Model for user feedback messages that appear on the screen with a certain severity. */
public final class DisplayableMessage {

  private final String message;
  private final Severity severity;

  public DisplayableMessage(String message, Severity severity) {
    this.message = message;
    this.severity = severity;
  }

  public String getMessage() {
    return message;
  }

  public Severity getSeverity() {
    return severity;
  }

  /** Standard severity levels for user feedback messages **/
  public enum Severity {
    ALERT(ToastType.ALERT),
    ERROR(ToastType.ERROR),
    SUCCESS(ToastType.SUCCESS),
    WARNING(ToastType.WARNING);

    private final ToastType toastType;

    Severity(ToastType toastType) {
      this.toastType = toastType;
    }

    public ToastType getToastType() {
      return toastType;
    }
  }
}
