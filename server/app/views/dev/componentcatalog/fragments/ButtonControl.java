package views.dev.componentcatalog.fragments;

public final class ButtonControl {

  public PrimaryButton primaryButton() {
    return new PrimaryButton("submitBtn", "Submit");
  }

  public SecondaryButton secondaryButton() {
    return new SecondaryButton("saveBtn", "save", "Save Draft", "submit", "secondary");
  }

  public OutlineButton outlineButton() {
    return new OutlineButton("cancelBtn", "Cancel", "outline", "big");
  }

  public DisabledButton disabledButton() {
    return new DisabledButton("processBtn", "Processing...", "accent-cool", "true", "true");
  }

  public DataButton dataButton() {
    return new DataButton(
        "deleteBtn",
        "action",
        "delete",
        "Delete",
        "submit",
        "base",
        "delete-record",
        "Are you sure you want to delete this?",
        "Delete this record");
  }

  public IconButton iconButton() {
    return new IconButton("menuBtn", "accent-warm", "Open menu", "Delete");
  }

  public record PrimaryButton(String id, String text) {}

  public record SecondaryButton(String id, String name, String text, String type, String variant) {}

  public record OutlineButton(String id, String text, String variant, String size) {}

  public record DisabledButton(
      String id, String text, String variant, String disabled, String size) {}

  public record DataButton(
      String id,
      String name,
      String value,
      String text,
      String type,
      String variant,
      String action,
      String confirm,
      String ariaLabel) {}

  public record IconButton(String id, String variant, String ariaLabel, String text) {}
}
