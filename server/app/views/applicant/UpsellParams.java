package views.applicant;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.LocalizedStrings;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService.ApplicantProgramData;

/** Params for rendering Upsell View. */
@AutoValue
public abstract class UpsellParams {

  public static Builder builder() {
    return new AutoValue_UpsellParams.Builder();
  }

  abstract Request request();

  abstract CiviFormProfile profile();

  abstract long applicantId();

  abstract long applicationId();

  abstract ApplicantPersonalInfo applicantPersonalInfo();

  abstract Optional<String> bannerMessage();

  // Use programTitle or eligiblePrograms, but not both
  abstract Optional<String> programTitle();

  abstract Optional<ImmutableList<ApplicantProgramData>> eligiblePrograms();

  // Will be non-empty if programTitle is used
  abstract Optional<String> programDescription();

  abstract LocalizedStrings customConfirmationMessage();

  // Localized string for the date the application was submitted
  abstract String dateSubmitted();

  abstract Messages messages();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setRequest(Request request);

    public abstract Builder setProfile(CiviFormProfile profile);

    public abstract Builder setApplicantId(long applicantId);

    public abstract Builder setApplicationId(long applicationId);

    public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo applicantPersonalInfo);

    public abstract Builder setBannerMessage(Optional<String> bannerMessage);

    public abstract Builder setProgramTitle(String programTitle);

    public abstract Builder setEligiblePrograms(
        ImmutableList<ApplicantProgramData> eligiblePrograms);

    public abstract Builder setProgramDescription(String programDescription);

    public abstract Builder setCustomConfirmationMessage(
        LocalizedStrings customConfirmationMessage);

    public abstract Builder setDateSubmitted(String dateSubmitted);

    public abstract Builder setMessages(Messages messages);

    public abstract UpsellParams build();
  }
}
