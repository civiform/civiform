package services.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Singleton
public class SimpleEmail {
  public static final String AWS_SES_SENDER_CONF_PATH = "aws.ses.sender";

  private final String sender;
  private final Client client;

  @Inject
  public SimpleEmail(
      AwsRegion region, Config config, Environment environment, ApplicationLifecycle appLifecycle) {
    this.sender = checkNotNull(config).getString(AWS_SES_SENDER_CONF_PATH);

    if (environment.isDev()) {
      client = new LocalStackClient(region, config);
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

    Destination destination = Destination.builder().toAddresses(toAddress).build();

    Body body = Body.builder().text(Content.builder().data(bodyText).build()).build();

    Message msg =
        Message.builder().subject(Content.builder().data(subject).build()).body(body).build();

    SendEmailRequest emailRequest =
        SendEmailRequest.builder().destination(destination).message(msg).source(sender).build();
    client.get().sendEmail(emailRequest);
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
    private static final String AWS_LOCAL_ENDPOINT_CONF_PATH = "aws.local.endpoint";

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
}
