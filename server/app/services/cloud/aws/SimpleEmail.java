package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.cloud.aws.AwsStorageUtils.AWS_LOCAL_ENDPOINT_CONF_PATH;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * SimpleEmail provides methods to send email notifications through AWS Simple Email Service (SES).
 */
@Singleton
public final class SimpleEmail {
  public static final String AWS_SES_SENDER_CONF_PATH = "aws.ses.sender";
  private static final Logger logger = LoggerFactory.getLogger(SimpleEmail.class);

  private static final Histogram EMAIL_EXECUTION_TIME =
      Histogram.build()
          .name("email_send_time_seconds")
          .help("Execution time of email send")
          .register();

  private static final Counter EMAIL_SEND_COUNT =
      Counter.build()
          .name("email_send_total")
          .help("Number of emails sent")
          .labelNames("status")
          .register();

  private static final Counter EMAIL_FAIL_COUNT =
      Counter.build()
          .name("email_fail_total")
          .help("Number of emails that failed to send")
          .register();

  private final String sender;
  private final Environment environment;
  private final Client client;

  @Inject
  public SimpleEmail(
      AwsRegion region, Config config, Environment environment, ApplicationLifecycle appLifecycle) {
    this.sender = checkNotNull(config).getString(AWS_SES_SENDER_CONF_PATH);
    this.environment = checkNotNull(environment);

    if (environment.isDev()) {
      client = new LocalStackClient(region, config);
    } else if (environment.isTest()) {
      client = new NullClient();
    } else {
      client = new AwsClient(region);
    }

    appLifecycle.addStopHook(
        () -> {
          client.close();
          return CompletableFuture.completedFuture(null);
        });
  }

  public void send(String toAddress, String subject, String bodyText) {
    send(ImmutableList.of(toAddress), subject, bodyText);
  }

  public void send(ImmutableList<String> toAddresses, String subject, String bodyText) {
    if (toAddresses.isEmpty()) {
      return;
    }
    Histogram.Timer timer = EMAIL_EXECUTION_TIME.startTimer();

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
      Destination destination =
          Destination.builder().toAddresses(toAddresses.toArray(new String[0])).build();

      Body body = Body.builder().text(Content.builder().data(bodyText).build()).build();

      Message msg =
          Message.builder().subject(Content.builder().data(subject).build()).body(body).build();

      SendEmailRequest emailRequest =
          SendEmailRequest.builder().destination(destination).message(msg).source(sender).build();
      client.get().sendEmail(emailRequest);
    } catch (SesException e) {
      logger.error(e.toString());
      e.printStackTrace();
      EMAIL_FAIL_COUNT.inc();
      EMAIL_SEND_COUNT.labels(String.valueOf(e.statusCode())).inc();
    } finally {
      // Increase the count of emails sent.
      EMAIL_SEND_COUNT.labels(String.valueOf(HttpStatusCode.OK)).inc();
      // Record the execution time of the email sending process.
      timer.observeDuration();
    }
  }

  interface Client {
    SesClient get();

    void close();
  }

  static class AwsClient implements Client {
    private final SesClient client;

    AwsClient(AwsRegion region) {
      client = SesClient.builder().region(region.get()).build();
    }

    @Override
    public SesClient get() {
      return client;
    }

    @Override
    public void close() {
      client.close();
    }
  }

  static class LocalStackClient implements Client {
    private final String localEndpoint;
    private final SesClient client;

    LocalStackClient(AwsRegion region, Config config) {
      localEndpoint = checkNotNull(config).getString(AWS_LOCAL_ENDPOINT_CONF_PATH);
      URI localUri;
      try {
        localUri = new URI(localEndpoint);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
      client = SesClient.builder().endpointOverride(localUri).region(region.get()).build();
    }

    @Override
    public SesClient get() {
      return client;
    }

    @Override
    public void close() {
      client.close();
    }
  }

  static class NullClient implements Client {
    private final SesClient client;

    NullClient() {
      client = Mockito.mock(SesClient.class);
    }

    @Override
    public SesClient get() {
      return client;
    }

    @Override
    public void close() {}
  }
}
