package services.cloud;

import java.util.Optional;

/** Enum representing supported options for cloud storage service. */
public enum StorageServiceName {
  /** Backwards compatibility, this default to AWS_S3 */
  S3("s3"),
  AWS_S3("aws-s3"),
  AZURE_BLOB("azure-blob"),
  GCP_S3("gcp-s3");
  private final String storageString;

  StorageServiceName(String storageString) {
    this.storageString = storageString;
  }

  /** Returns the enum associated with the provided string value */
  public static Optional<StorageServiceName> forString(String string) {
    for (StorageServiceName service : StorageServiceName.values()) {
      if (service.getString().equals(string)) {
        return Optional.of(service);
      }
    }
    return Optional.empty();
  }

  /** Returns the string value associated with the enum */
  public String getString() {
    return storageString;
  }
}
