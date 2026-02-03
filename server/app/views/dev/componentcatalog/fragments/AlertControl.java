package views.dev.componentcatalog.fragments;

public class AlertControl {
  public record AlertTag(String type, boolean slim, boolean noIcon, String headingType) {
    public static AlertTag createAlert(String type) {
      return new AlertTag(type, false, false, "h2");
    }
  }

  public AlertTag d() {
    return AlertTag.createAlert("info");
  }
}
