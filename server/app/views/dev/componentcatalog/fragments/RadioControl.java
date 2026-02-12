package views.dev.componentcatalog.fragments;

import com.google.common.collect.ImmutableList;

public final class RadioControl {

  public BasicRadio basicRadio() {
    return new BasicRadio(
        "size",
        "Choose a size",
        ImmutableList.of(
            new RadioOption("size-small", "small", "Small"),
            new RadioOption("size-medium", "medium", "Medium"),
            new RadioOption("size-large", "large", "Large")));
  }

  public RadioWithHelp radioWithHelp() {
    return new RadioWithHelp(
        "contactMethod",
        "How would you like to be contacted?",
        "Select your preferred method of communication",
        ImmutableList.of(
            new RadioOption("contact-email", "email", "Email"),
            new RadioOption("contact-phone", "phone", "Phone"),
            new RadioOption("contact-mail", "mail", "Mail")));
  }

  public RadioWithError radioWithError() {
    return new RadioWithError(
        "paymentMethod",
        "Payment Method",
        "false",
        "Please select a payment method",
        ImmutableList.of(
            new RadioOption("payment-card", "card", "Credit Card"),
            new RadioOption("payment-bank", "bank", "Bank Transfer"),
            new RadioOption("payment-paypal", "paypal", "PayPal")));
  }

  public RadioWithDescriptions radioWithDescriptions() {
    return new RadioWithDescriptions(
        "subscription",
        "Choose a subscription plan",
        ImmutableList.of(
            new RadioOptionWithDescription(
                "sub-basic", "basic", "Basic", "$9.99/month - Access to basic features"),
            new RadioOptionWithDescription(
                "sub-pro", "pro", "Professional", "$29.99/month - All features included"),
            new RadioOptionWithDescription(
                "sub-enterprise",
                "enterprise",
                "Enterprise",
                "Custom pricing - Dedicated support")));
  }

  public RadioNonTiled radioNonTiled() {
    return new RadioNonTiled(
        "delivery",
        "Delivery Speed",
        "false",
        ImmutableList.of(
            new RadioOptionWithSelected(
                "delivery-standard", "standard", "Standard (5-7 days)", true),
            new RadioOptionWithSelected("delivery-express", "express", "Express (2-3 days)", false),
            new RadioOptionWithSelected("delivery-overnight", "overnight", "Overnight", false)));
  }

  public record BasicRadio(String name, String label, ImmutableList<RadioOption> options) {}

  public record RadioWithHelp(
      String name, String label, String helpText, ImmutableList<RadioOption> options) {}

  public record RadioWithError(
      String name,
      String label,
      String isValid,
      String validationMessage,
      ImmutableList<RadioOption> options) {}

  public record RadioWithDescriptions(
      String name, String label, ImmutableList<RadioOptionWithDescription> options) {}

  public record RadioNonTiled(
      String name, String label, String tiled, ImmutableList<RadioOptionWithSelected> options) {}

  public record RadioOption(String id, String value, String label) {}

  public record RadioOptionWithDescription(
      String id, String value, String label, String description) {}

  public record RadioOptionWithSelected(String id, String value, String label, boolean selected) {}
}
