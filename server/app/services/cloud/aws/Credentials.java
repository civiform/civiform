package services.cloud.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * This class retrieves AWS credentials through default provider.
 *
 * <p>See
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain
 * for more details.
 */
public final class Credentials {

  private static final DefaultCredentialsProvider credentialsProvider =
      DefaultCredentialsProvider.create();

  public AwsCredentials getCredentials() {
    return credentialsProvider.resolveCredentials();
  }

  public DefaultCredentialsProvider credentialsProvider() {
    return credentialsProvider;
  }
}
