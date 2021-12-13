package services.cloud;

public enum StorageServiceName {
  AWS_S3("s3"),
  AZURE_BLOB("azure-blob"),
  ;
  private final String storageString;

  StorageServiceName(String storageString) {
    this.storageString = storageString;
  }

  public String getString() {
    return storageString;
  }
}
