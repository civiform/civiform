package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.URI;
import org.junit.Test;
import repository.ResetPostgres;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public class AwsS3ClientTest extends ResetPostgres {
  private static final URI endpointUri = URI.create("https://s3.us-east-2.amazonaws.com");
  private final Credentials credentials = instanceOf(Credentials.class);
  private final AwsS3Client awsS3Client = new AwsS3Client();

  @Test
  public void deleteObject_noKeyInRequest_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                awsS3Client.deleteObject(
                    credentials,
                    Region.US_EAST_2,
                    endpointUri,
                    DeleteObjectRequest.builder().key("").bucket("fakeBucket").build()))
        .withMessageContaining("must have a key");
  }

  @Test
  public void deleteObject_noBucketInRequest_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                awsS3Client.deleteObject(
                    credentials,
                    Region.US_EAST_2,
                    endpointUri,
                    DeleteObjectRequest.builder().key("fakeKey").bucket("").build()))
        .withMessageContaining("must have a bucket");
  }
}
