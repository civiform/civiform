package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import play.Environment;
import services.email.EmailSendClient;
import services.email.EmailSendProvider;
import services.email.aws.SimpleEmail;

/** Configures and initializes the classes for interacting with email sending. */
public class EmailSendModule extends AbstractModule {
  private final Config config;

  // Environment must always be provided as a param, even if it's unused.
  public EmailSendModule(Environment unused, Config config) {
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    EmailSendProvider emailSendProvider;
    try {
      String emailProvider = checkNotNull(config).getString("email.provider");
      emailSendProvider =
          EmailSendProvider.forString(emailProvider).orElse(EmailSendProvider.AWS_SES);
    } catch (ConfigException ex) {
      // Default to AWS SES if nothing is configured
      emailSendProvider = EmailSendProvider.AWS_SES;
    }

    switch (emailSendProvider) {
      case AWS_SES:
        bind(EmailSendClient.class).to(SimpleEmail.class);
        break;
      case GRAPH_API:
        break;
    }
  }
}
