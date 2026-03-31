package parsers;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.cloud.StorageServiceName;

// Class representing the result of a streaming multipart upload operation, including its status
// (success or failure).
// TODO: Extend this class to include file data.
@AutoValue
public abstract class StreamingMultipartUploadResult {
  public enum Status {
    SUCCESS,
    FAILURE,
    NOT_IMPLEMENTED // Should not reach this state
  }

  public abstract Status getStatus();

  public abstract StorageServiceName getStorageServiceName();

  public abstract Optional<String> getStoredFilePath();

  public abstract Optional<String> getErrorMessage();

  public static Builder builder() {
    return new AutoValue_StreamingMultipartUploadResult.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setStatus(Status status);

    public abstract Builder setStorageServiceName(StorageServiceName storageServiceName);

    public abstract Builder setStoredFilePath(Optional<String> storedFilePath);

    public abstract Builder setErrorMessage(Optional<String> errorMessage);

    public abstract StreamingMultipartUploadResult build();
  }
}
