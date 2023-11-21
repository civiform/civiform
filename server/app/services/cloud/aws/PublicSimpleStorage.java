package services.cloud.aws;

import com.typesafe.config.Config;
import org.mockito.Mockito;
import play.Environment;
import play.inject.ApplicationLifecycle;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PublicSimpleStorage implements PublicStorageClient {
  private static final String AWS_PUBLIC_S3_BUCKET_CONF_PATH = "aws.s3.publicbucket";
  private static final Duration AWS_PRESIGNED_URL_DURATION = Duration.ofMinutes(10);
  private static final int AWS_S3_FILE_LIMIT_CONF_PATH = 1;

  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final Client client;

  @Inject
  public PublicSimpleStorage(
    AwsRegion region,
    Credentials credentials,
    Config config,
    Environment environment,
    ApplicationLifecycle appLifecycle
    ) {
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(AWS_PUBLIC_S3_BUCKET_CONF_PATH);
    if (environment.isDev()) {
      client = new LocalStackClient(config);
    } else if (environment.isTest()) {
      client = new NullClient();
    } else {
      client = new AwsClient();
    }

    appLifecycle.addStopHook(
      () -> {
        client.close();
        return CompletableFuture.completedFuture(null);
      });
  }

  @Override
  public StorageUploadRequest getSignedUploadRequest(String fileName, String successRedirectActionLink) {
    AwsCredentials awsCredentials = credentials.getCredentials();
    SignedS3UploadRequest.Builder builder =
      SignedS3UploadRequest.builder()
        .setActionLink(client.bucketAddress())
        .setKey(fileName)
        .setSuccessActionRedirect(successRedirectActionLink)
        .setAccessKey(awsCredentials.accessKeyId())
        .setExpirationDuration(AWS_PRESIGNED_URL_DURATION)
        .setBucket(bucket)
        .setSecretKey(awsCredentials.secretAccessKey())
        .setRegionName(region.id())
        .setFileLimitMb(AWS_S3_FILE_LIMIT_CONF_PATH);
    if (awsCredentials instanceof AwsSessionCredentials) {
      AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) awsCredentials;
      builder.setSecurityToken(sessionCredentials.sessionToken());
    }
    return builder.build();
  }

  @Override
  public String getDisplayUrl(String fileKey) {
    return client.bucketAddress() + "/" + fileKey;
  }

  interface Client {
    String bucketAddress();

    void close();
  }

  static class NullClient implements Client {

    NullClient() {
    }

    @Override
    public String bucketAddress() {
      return "fake-bucket-address";
    }

    @Override
    public void close() {}
  }

  class AwsClient implements Client {


    AwsClient() {
    }

    @Override
    public String bucketAddress() {
      return String.format("https://%s.s3.%s.amazonaws.com/", bucket, region.id());
    }

    @Override
    public void close() {
    }
  }

  class LocalStackClient implements Client {

    private static final String AWS_LOCAL_ENDPOINT_CONF_PATH = "aws.local.endpoint";

    private final String localS3Endpoint;

    LocalStackClient(Config config) {
      String localEndpoint = checkNotNull(config).getString(AWS_LOCAL_ENDPOINT_CONF_PATH);
      try {
        URI localUri = new URI(localEndpoint);
        localS3Endpoint =
          String.format("%s://s3.%s", localUri.getScheme(), localUri.getAuthority());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String bucketAddress() {
      try {
        return S3EndpointProvider.defaultProvider()
          .resolveEndpoint(
            (builder) -> builder.endpoint(localS3Endpoint).bucket(bucket).region(region))
          .get()
          .url()
          .toString();
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() {
    }
  }
}
