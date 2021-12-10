package services.cloud;

public enum StorageService {
  AWS_S3("s3"),
  AZURE_BLOB("azure-blob"),
  ;
  private final String storageString;

  StorageService(String storageString) {
    this.storageString = storageString;
  }

  public String getString() {
    return storageString;
  }
}