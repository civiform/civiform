package repository;


import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class AmazonS3ClientTest extends WithPostgresContainer {

  @Test
  public void testErrorOnPutObjectNull() {
    AmazonS3ClientNull s3ClientMock = new AmazonS3ClientNull();

    boolean error = false;

    try {
      String testInput = "UAT S3 test content";
      s3ClientMock.putObject("file2", testInput.getBytes(StandardCharsets.UTF_8));
    } catch (RuntimeException e) {
      error = true;
    }

    if (!error) {
      throw new RuntimeException("null s3 client did not throw io exception");
    }
  }

  @Test
  public void testErrorOnGetObjectNull() {
    AmazonS3ClientNull s3ClientMock = new AmazonS3ClientNull();

    boolean error = false;

    try {
      s3ClientMock.getObject("file2");
    } catch (RuntimeException e) {
      error = true;
    }

    if (!error) {
      throw new RuntimeException("null s3 client did not throw io exception");
    }
  }
}
