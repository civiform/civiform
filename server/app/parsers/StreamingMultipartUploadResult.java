package parsers;

import com.google.auto.value.AutoValue;

// Class representing the result of a streaming multipart upload operation, including its status
// (success or failure).
// TODO: Extend this class to include file data.
@AutoValue
public abstract class StreamingMultipartUploadResult {
  public enum Status {
    SUCCESS,
    FAILURE
  }

  public abstract Status getStatus();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setStatus(Status status);

    public abstract StreamingMultipartUploadResult build();
  }
}
