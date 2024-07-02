package services;

/**
 * Sets the alert level for displaying alerts to users. To be used in concert with USWDS alerts as
 * defined for for {@link views.ViewUtils#makeAlert}
 */
public enum AlertType {
  NONE,
  ERROR,
  INFO,
  SUCCESS,
  WARNING
}
