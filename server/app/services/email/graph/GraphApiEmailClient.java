package services.email.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import com.microsoft.kiota.ApiException;
import com.typesafe.config.Config;
import io.prometheus.client.Histogram;
import java.util.ArrayList;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.cloud.azure.Credentials;
import services.email.EmailSendClient;
import services.monitoring.EmailSendMetrics;
import software.amazon.awssdk.http.HttpStatusCode;

/** GraphApiEmailClient provides methods to send email notifications through Microsoft Graph API. */
@Singleton
public class GraphApiEmailClient implements EmailSendClient {
  public static final String AZURE_SENDER_CONF_PATH = "email.sender";
  private static final Logger logger = LoggerFactory.getLogger(GraphApiEmailClient.class);

  private final EmailSendMetrics emailSendMetrics;
  private final GraphApiClientInterface client;
  private final Environment environment;
  private final String sender;

  @Inject
  public GraphApiEmailClient(
      Credentials credentials,
      Config config,
      Environment environment,
      EmailSendMetrics emailSendMetrics) {
    this.environment = checkNotNull(environment);
    this.sender = checkNotNull(config).getString(AZURE_SENDER_CONF_PATH);
    this.emailSendMetrics = emailSendMetrics;

    if (environment.isProd()) {
      client = new GraphApiClient(credentials);
    } else {
      client = new TestGraphApiClient();
    }
  }

  @Override
  public void send(String toAddress, String subject, String bodyText) {
    logger.info("In graph api email send ");
    send(ImmutableList.of(toAddress), subject, bodyText);
  }

  @Override
  public void send(ImmutableList<String> toAddresses, String subject, String bodyText) {
    logger.info("In graph api email send ");
    if (toAddresses.isEmpty()) {
      logger.info("To addresses are empty ");
      return;
    }
    Histogram.Timer timer = emailSendMetrics.getEmailExecutionTime().startTimer();

    // Add some messaging to non-prod emails to make it easier to
    // tell that it's not a prod notification.
    if (!environment.isProd()) {

      subject = String.format("[Test Message] %s", subject);
      bodyText =
          String.format(
              "This email was generated from our test server.\n\n"
                  + "If you didn't expect this message please disregard.\n\n"
                  + "***************************************************\n\n\n"
                  + "%s",
              bodyText);
    }

    try {
      logger.info("Attempting send");
      Message message = new Message();
      message.setSubject(subject);
      ItemBody body = new ItemBody();
      body.setContentType(BodyType.Text);
      body.setContent(bodyText);
      message.setBody(body);

      Recipient senderRecipient = new Recipient();
      EmailAddress senderEmail = new EmailAddress();
      senderEmail.setAddress(sender);
      senderRecipient.setEmailAddress(senderEmail);
      message.setFrom(senderRecipient);

      ArrayList<Recipient> toRecipients = new ArrayList<Recipient>();
      toAddresses.forEach(
          address -> {
            Recipient recipient = new Recipient();
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.setAddress(address);
            recipient.setEmailAddress(emailAddress);
            toRecipients.add(recipient);
          });
      message.setToRecipients(toRecipients);

      SendMailPostRequestBody sendMailPostRequestBody = new SendMailPostRequestBody();
      sendMailPostRequestBody.setMessage(message);
      // This can be a configurable value if that is something the team wants.
      sendMailPostRequestBody.setSaveToSentItems(false);
      client
          .get()
          .users()
          .byUserId("civiform-dev_exygy.com#EXT#@civiformdevexygy.onmicrosoft.com")
          .sendMail()
          .post(sendMailPostRequestBody);
    } catch (ApiException e) {
      logger.error(e.toString());
      e.printStackTrace();
      emailSendMetrics.getEmailFailCount().inc();
      emailSendMetrics.getEmailSendCount().labels(String.valueOf(e.getResponseStatusCode())).inc();
    } finally {
      // Increase the count of emails sent.
      emailSendMetrics.getEmailSendCount().labels(String.valueOf(HttpStatusCode.OK)).inc();
      // Record the execution time of the email sending process.
      timer.observeDuration();
    }
  }

  @VisibleForTesting
  GraphApiClientInterface getClient() {
    return client;
  }
}
