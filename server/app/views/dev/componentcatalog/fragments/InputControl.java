package views.dev.componentcatalog.fragments;

public final class InputControl {

  public BasicTextInput basicInput() {
    return new BasicTextInput("firstName", "firstName", "First Name", "Enter your first name");
  }

  public EmailInput emailInput() {
    return new EmailInput(
        "userEmail",
        "email",
        "Email Address",
        "email",
        "We'll never share your email with anyone else.",
        "name@example.com");
  }

  public PasswordInput passwordInput() {
    return new PasswordInput(
        "userPassword",
        "password",
        "Password",
        "password",
        "false",
        "Password must be at least 8 characters");
  }

  public NumberInput numberInput() {
    return new NumberInput("age", "age", "Age", "number", "25", "You must be 18 or older");
  }

  public DataAttributeInput dataInput() {
    return new DataAttributeInput(
        "searchQuery", "q", "Search", "search-input", "search-initiated", "Search our website");
  }

  public record BasicTextInput(String id, String name, String label, String placeholder) {}

  public record EmailInput(
      String id, String name, String label, String type, String helpText, String placeholder) {}

  public record PasswordInput(
      String id,
      String name,
      String label,
      String type,
      String isValid,
      String validationMessage) {}

  public record NumberInput(
      String id, String name, String label, String type, String value, String helpText) {}

  public record DataAttributeInput(
      String id,
      String name,
      String label,
      String testId,
      String analyticsEvent,
      String ariaLabel) {}
}
