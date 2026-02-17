package views.dev.componentcatalog.fragments;

public final class TextareaControl {

  public BasicTextarea basicTextarea() {
    return new BasicTextarea("comments", "comments", "Comments", "Enter your comments here");
  }

  public TextareaWithHelp textareaWithHelp() {
    return new TextareaWithHelp(
        "description",
        "description",
        "Project Description",
        "Provide a detailed description of your project",
        "5",
        "Describe your project...");
  }

  public TextareaWithError textareaWithError() {
    return new TextareaWithError(
        "feedback",
        "feedback",
        "Feedback",
        "false",
        "Feedback must be at least 10 characters",
        "4");
  }

  public TextareaWithValue textareaWithValue() {
    return new TextareaWithValue(
        "bio",
        "bio",
        "Biography",
        "I am a software developer with 5 years of experience.",
        "6",
        "50",
        "Tell us about yourself");
  }

  public TextareaWithData textareaWithData() {
    return new TextareaWithData(
        "message", "message", "Message", "message-textarea", "500", "Enter your message here", "8");
  }

  public record BasicTextarea(String id, String name, String label, String placeholder) {}

  public record TextareaWithHelp(
      String id, String name, String label, String helpText, String rows, String placeholder) {}

  public record TextareaWithError(
      String id,
      String name,
      String label,
      String isValid,
      String validationMessage,
      String rows) {}

  public record TextareaWithValue(
      String id,
      String name,
      String label,
      String value,
      String rows,
      String cols,
      String helpText) {}

  public record TextareaWithData(
      String id,
      String name,
      String label,
      String testId,
      String maxLength,
      String ariaLabel,
      String rows) {}
}
