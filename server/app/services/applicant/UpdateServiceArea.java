package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.Path;
import services.geo.ServiceAreaInclusion;

/** Represents the service area piece of the applicant's answers to an address question. */
@AutoValue
public abstract class UpdateServiceArea {
  public static UpdateServiceArea create(Path path, ImmutableList<ServiceAreaInclusion> value) {
    return new AutoValue_UpdateServiceArea(path, value);
  }

  /**
   * A JSON-style path pointing to a scalar value to update in the applicant's {@link
   * ApplicantData}.
   */
  abstract Path path();

  /** The value to update the the applicant's {@link ApplicantData} to. */
  abstract ImmutableList<ServiceAreaInclusion> value();
}
