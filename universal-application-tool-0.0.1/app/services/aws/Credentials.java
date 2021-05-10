package services.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class Credentials {

  private static final DefaultCredentialsProvider credentialsProvider =
      DefaultCredentialsProvider.create();

  public AwsCredentials getCredentials() {
    return credentialsProvider.resolveCredentials();
  }
}
