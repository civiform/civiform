package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.URI;
import org.junit.Test;
import repository.ResetPostgres;
import services.cloud.generic_s3.GenericS3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

public class AwsS3ClientTest extends ResetPostgres {
  private static final URI endpointUri = URI.create("https://s3.us-east-2.amazonaws.com");
  private final Credentials credentials = instanceOf(Credentials.class);
  private final GenericS3Client awsS3Client = new GenericS3Client();

  @Test
  public void deleteObjects_noObjectsInRequest_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                awsS3Client.deleteObjects(
                    credentials,
                    Region.US_EAST_2,
                    endpointUri,
                    DeleteObjectsRequest.builder()
                        .bucket("fakeBucket")
                        .delete(Delete.builder().build())
                        .build()))
        .withMessageContaining("must have at least one object");
  }

  @Test
  public void deleteObjects_noBucketInRequest_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                awsS3Client.deleteObjects(
                    credentials,
                    Region.US_EAST_2,
                    endpointUri,
                    DeleteObjectsRequest.builder()
                        .bucket("")
                        .delete(
                            Delete.builder()
                                .objects(ObjectIdentifier.builder().key("key").build())
                                .build())
                        .build()))
        .withMessageContaining("must have a bucket");
  }

  // It's difficult to test the other functionality in AwsS3Client because S3Client will try and
  // connect to a functioning AWS endpoint, which we don't want to stand up for a unit test. There
  // are browser tests that test file upload, download, and deletion that do use a functioning AWS
  // endpoint and should cover the rest of the AwsS3Client implementation.
}
