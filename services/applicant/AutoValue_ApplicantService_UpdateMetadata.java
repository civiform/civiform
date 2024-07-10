package services.applicant;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ApplicantService_UpdateMetadata extends ApplicantService.UpdateMetadata {

  private final long programId;

  private final long updatedAt;

  AutoValue_ApplicantService_UpdateMetadata(long programId, long updatedAt) {
    this.programId = programId;
    this.updatedAt = updatedAt;
  }

  @Override
  long programId() {
    return programId;
  }

  @Override
  long updatedAt() {
    return updatedAt;
  }

  @Override
  public String toString() {
    return "UpdateMetadata{" + "programId=" + programId + ", " + "updatedAt=" + updatedAt + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ApplicantService.UpdateMetadata) {
      ApplicantService.UpdateMetadata that = (ApplicantService.UpdateMetadata) o;
      return this.programId == that.programId() && this.updatedAt == that.updatedAt();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((programId >>> 32) ^ programId);
    h$ *= 1000003;
    h$ ^= (int) ((updatedAt >>> 32) ^ updatedAt);
    return h$;
  }
}
