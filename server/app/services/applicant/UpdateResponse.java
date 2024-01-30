package services.applicant;

import com.google.auto.value.AutoValue;

/** A class representing the response after saving an applicant's updated answers. */
@AutoValue
public abstract class UpdateResponse {
  public static UpdateResponse create(
      ReadOnlyApplicantProgramService readOnlyApplicantProgramService, boolean answersChanged) {
    return new AutoValue_UpdateResponse(readOnlyApplicantProgramService, answersChanged);
  }

  /** An instance of {@link ReadOnlyApplicantProgramService} with the updated answers. */
  public abstract ReadOnlyApplicantProgramService readOnlyApplicantProgramService();

  /**
   * True if the applicant's answers have changed since the last time they updated them and false
   * otherwise.
   */
  public abstract boolean answersChanged();
}
