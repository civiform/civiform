package views.applicant.support;

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

  public abstract Request request();

  public abstract CiviFormProfile profile();

  public abstract long applicantId();

  public abstract long applicationId();

  public abstract ApplicantPersonalInfo applicantPersonalInfo();

  public abstract Optional<String> bannerMessage();

  // Use programTitle or eligiblePrograms, but not both
  public abstract Optional<String> programTitle();

  public abstract Optional<ImmutableList<ApplicantProgramData>> eligiblePrograms();

  public abstract Optional<String> programShortDescription();

  // Program ID of the program that was just applied to
  public abstract long completedProgramId();

  public abstract LocalizedStrings customConfirmationMessage();

  // Localized string for the date the application was submitted
  public abstract String dateSubmitted();

  public abstract Messages messages();

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

    public abstract Builder setProgramShortDescription(String programShortDescription);

    public abstract Builder setCompletedProgramId(long programId);

    public abstract Builder setCustomConfirmationMessage(
        LocalizedStrings customConfirmationMessage);

    public abstract Builder setDateSubmitted(String dateSubmitted);

    public abstract Builder setMessages(Messages messages);

    public abstract UpsellParams build();
  }
}
