package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.email.EmailSendClient;
import services.email.EmailSendProvider;
// import services.email.aws.SimpleEmail;
import services.email.graph.GraphApiEmailClient;

/** Configures and initializes the classes for interacting with email sending. */
public class EmailSendModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(EmailSendModule.class);
  private final Config config;

  // Environment must always be provided as a param, even if it's unused.
  public EmailSendModule(Environment unused, Config config) {
    logger.info("In email send module");
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    logger.info("In configure email send module");
    EmailSendProvider emailSendProvider;
    try {
      String emailProvider = checkNotNull(config).getString("email.provider");
      logger.info("The email provider from the config is " + emailProvider);
      emailSendProvider =
          EmailSendProvider.fromString(emailProvider).orElse(EmailSendProvider.AWS_SES);
    } catch (ConfigException ex) {
      // Default to AWS SES if nothing is configured
      logger.warn("No email provider specified. Defaulting to graph");
      emailSendProvider = EmailSendProvider.GRAPH_API;
    }

    switch (emailSendProvider) {
      case AWS_SES:
        logger.info("Got to switch condition " + emailSendProvider);
        bind(EmailSendClient.class).to(GraphApiEmailClient.class);
        break;
      case GRAPH_API:
        logger.info("Got to switch condition " + emailSendProvider);
        bind(EmailSendClient.class).to(GraphApiEmailClient.class);
        break;
      default:
        logger.info("Got to default condition " + emailSendProvider);
        bind(EmailSendClient.class).to(GraphApiEmailClient.class);
        break;
    }
  }
}
