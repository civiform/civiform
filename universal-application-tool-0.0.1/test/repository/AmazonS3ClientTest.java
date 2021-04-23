package repository;

import static org.mockito.Mockito.mock;

public class AmazonS3ClientTest extends WithPostgresContainer {

  @Test
  public void testNullClient() {
    AmazonS3Client s3ClientMock = mock(AmazonS3ClientNull.class);
  }
}
