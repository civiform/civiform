package repository;

import org.junit.Test;
import static org.mockito.Mockito.mock;
import repository.AmazonS3Client;
import java.nio.charset.StandardCharsets;

public class AmazonS3ClientTest extends WithPostgresContainer {

  @Test
  public void testNullClient() {
    AmazonS3Client s3ClientMock = mock(AmazonS3Client.class);

    try {
      String testInput = "UAT S3 test content";
      s3ClientMock.putObject("file2", testInput.getBytes(StandardCharsets.UTF_8));
    } catch (RuntimeException e) {
      // Everything is good
      return;
    }

    throw new RuntimeException("null s3 client did not throw io exception");
  }
}
