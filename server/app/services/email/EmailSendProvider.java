package services.email;

import java.util.Optional;

/** Enum representing supported options for email providers. */
public enum EmailSendProvider {
  AWS_SES("aws-ses"),
  GRAPH_API("graph-api");

  private final String emailProvider;

  EmailSendProvider(String emailProvider) {
    this.emailProvider = emailProvider;
  }

  /** Returns the enum associated with the provided string value */
  public static Optional<EmailSendProvider> fromString(String string) {
    for (EmailSendProvider service : EmailSendProvider.values()) {
      if (service.getString().equals(string)) {
        return Optional.of(service);
      }
    }
    return Optional.empty();
  }

  /** Returns the string value associated with the enum */
  public String getString() {
    return emailProvider;
  }
}
