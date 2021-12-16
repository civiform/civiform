package services.cloud.aws;

import static org.mockito.Mockito.when;

import javax.inject.Inject;
import org.mockito.Mockito;
import play.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * This class retrieves AWS credentials through default provider.
 *
 * <p>See
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain
 * for more details.
 */
public class Credentials {

  private final AwsCredentials credentials;

  @Inject
  Credentials(Environment environment) {
    if (environment.isTest()) {
      credentials = Mockito.mock(AwsCredentials.class);
      when(credentials.secretAccessKey()).thenReturn("secretAccessKey");
      when(credentials.accessKeyId()).thenReturn("accessKeyId");
    } else {
      credentials = DefaultCredentialsProvider.create().resolveCredentials();
    }
  }

  public AwsCredentials getCredentials() {
    return credentials;
  }
}
