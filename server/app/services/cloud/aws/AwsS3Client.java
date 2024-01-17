package services.cloud.aws;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.net.URI;
import java.util.stream.Collectors;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A real implementation of {@link AwsS3ClientWrapper} that delegates to the real {@link S3Client}.
 */
public class AwsS3Client implements AwsS3ClientWrapper {
  @Inject
  public AwsS3Client() {}

  @Override
  public void deleteObject(
      Credentials credentials, Region region, URI endpoint, DeleteObjectRequest request)
      throws FileDeletionFailureException {
    if (request.key().isBlank()) {
      throw new IllegalArgumentException("The request must have a key.");
    }
    if (request.bucket().isBlank()) {
      throw new IllegalArgumentException("The request must have a bucket.");
    }
    try (S3Client s3Client = createS3Client(credentials, region, endpoint)) {
      try {
        s3Client.deleteObject(request);
      } catch (AwsServiceException | SdkClientException e) {
        // AwsServiceException: The call was transmitted successfully, but AWS S3 couldn't process
        // it for some reason.
        // SdkClientException: AWS S3 couldn't be contacted for a response or the client couldn't
        // parse the response from AWS S3.
        // See https://docs.aws.amazon.com/AmazonS3/latest/userguide/delete-objects.html.
        throw new FileDeletionFailureException(e);
      }
    }
  }

  @Override
  public ImmutableList<String> listObjects(
          Credentials credentials, Region region, URI endpoint, ListObjectsV2Request listObjectsV2Request
  ) {
    try (S3Client s3Client = createS3Client(credentials, region, endpoint)) {
      try {
        ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsV2Request);
        return response.contents().stream().map(S3Object::key).collect(ImmutableList.toImmutableList());
      } catch (AwsServiceException | SdkClientException e) {
        // AwsServiceException: The call was transmitted successfully, but AWS S3 couldn't process
        // it for some reason.
        // SdkClientException: AWS S3 couldn't be contacted for a response or the client couldn't
        // parse the response from AWS S3.
        // See https://docs.aws.amazon.com/AmazonS3/latest/userguide/delete-objects.html.
        // TODO
      }
    }
    return ImmutableList.of();
  }

  private S3Client createS3Client(Credentials credentials, Region region, URI endpoint) {
    return S3Client.builder()
        .credentialsProvider(credentials.credentialsProvider())
        .region(region)
        // Override the endpoint so that Localstack works correctly.
        // See https://docs.localstack.cloud/user-guide/integrations/sdks/java/#s3-service and
        // https://github.com/localstack/localstack/issues/5209#issuecomment-1004395805.
        .endpointOverride(endpoint)
        .build();
  }
}
