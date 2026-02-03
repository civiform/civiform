package views.dev.componentcatalog.fragments;

import com.google.common.collect.ImmutableList;

public class CheckboxControl {

  public BasicCheckbox basicCheckbox() {
    return new BasicCheckbox(
        "interests-id",
        "interests",
        "Select your interests",
        ImmutableList.of(
            new BasicCheckbox.Option("interest-reading", "reading", "Reading"),
            new BasicCheckbox.Option("interest-sports", "sports", "Sports"),
            new BasicCheckbox.Option("interest-music", "music", "Music")));
  }

  public CheckboxWithHelp checkboxWithHelp() {
    return new CheckboxWithHelp(
        "notifications-id",
        "notifications",
        "Email notifications",
        "Select which types of emails you'd like to receive",
        ImmutableList.of(
            new CheckboxWithHelp.Option(
                "notif-updates",
                "updates",
                "Product updates",
                "News about product features and updates"),
            new CheckboxWithHelp.Option(
                "notif-newsletter",
                "newsletter",
                "Newsletter",
                "Monthly newsletter with tips and stories"),
            new CheckboxWithHelp.Option(
                "notif-offers", "offers", "Promotional offers", "Exclusive deals and discounts")));
  }

  public CheckboxWithError checkboxWithError() {
    return new CheckboxWithError(
        "terms-id",
        "terms",
        "Terms and conditions",
        "false",
        "You must accept at least one term to continue",
        ImmutableList.of(
            new CheckboxWithError.Option(
                "terms-service", "service", "I agree to the Terms of Service"),
            new CheckboxWithError.Option(
                "terms-privacy", "privacy", "I agree to the Privacy Policy")));
  }

  public CheckboxSelected checkboxSelected() {
    return new CheckboxSelected(
        "skills-id",
        "skills",
        "Programming skills",
        "false",
        ImmutableList.of(
            new CheckboxSelected.Option("skill-java", "java", "Java", true),
            new CheckboxSelected.Option("skill-python", "python", "Python", true),
            new CheckboxSelected.Option("skill-javascript", "javascript", "JavaScript", false),
            new CheckboxSelected.Option("skill-csharp", "csharp", "C#", false)));
  }

  public CheckboxWithData checkboxWithData() {
    return new CheckboxWithData(
        "features-id",
        "features",
        "Available features",
        ImmutableList.of(
            new CheckboxWithData.Option(
                "feature-basic",
                "basic",
                "Basic features",
                "checkbox-basic",
                "feature-basic-selected",
                false),
            new CheckboxWithData.Option(
                "feature-premium",
                "premium",
                "Premium features",
                "checkbox-premium",
                "feature-premium-selected",
                true)));
  }

  public record BasicCheckbox(
      String id, String name, String label, ImmutableList<BasicCheckbox.Option> items) {
    public record Option(String id, String value, String label) {}
  }

  public record CheckboxWithHelp(
      String id,
      String name,
      String label,
      String helpText,
      ImmutableList<CheckboxWithHelp.Option> items) {
    public record Option(String id, String value, String label, String description) {}
  }

  public record CheckboxWithError(
      String id,
      String name,
      String label,
      String isValid,
      String validationMessage,
      ImmutableList<CheckboxWithError.Option> items) {
    public record Option(String id, String value, String label) {}
  }

  public record CheckboxSelected(
      String id,
      String name,
      String label,
      String tiled,
      ImmutableList<CheckboxSelected.Option> items) {
    public record Option(String id, String value, String label, boolean selected) {}
  }

  public record CheckboxWithData(
      String id, String name, String label, ImmutableList<CheckboxWithData.Option> items) {
    public record Option(
        String id, String value, String label, String testId, String analytics, boolean disabled) {}
  }
}
