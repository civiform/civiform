package repository;

public class AmazonS3ClientNull extends AmazonS3Client {

  @Override
  public boolean enabled() {
    // Mock enabled value
    return true;
  }

  @Override
  public void ensureS3Client() {
    return;
  }
}
