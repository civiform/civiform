package views;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.Block;
import services.cloud.ApplicantStorageClient;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererParams;

@AutoValue
public abstract class ApplicationBaseViewParams {
  public static Builder builder() {
    return new AutoValue_ApplicationBaseViewParams.Builder();
  }

  public abstract Builder toBuilder();

  public abstract boolean inReview();

  public abstract Http.Request request();

  public abstract Messages messages();

  public abstract int blockIndex();

  public abstract int totalBlockCount();

  public abstract long applicantId();

  public abstract String programTitle();

  public abstract long programId();

  public abstract Block block();

  public abstract boolean preferredLanguageSupported();

  public abstract ApplicantStorageClient applicantStorageClient();

  public abstract String baseUrl();

  public abstract ApplicantPersonalInfo applicantPersonalInfo();

  public abstract ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode();

  public abstract Optional<ToastMessage> bannerMessage();

  public abstract Optional<String> applicantSelectedQuestionName();

  public abstract ApplicantRoutes applicantRoutes();

  public abstract CiviFormProfile profile();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setRequest(Http.Request request);

    public abstract Builder setInReview(boolean inReview);

    public abstract Builder setMessages(Messages messages);

    public abstract Builder setBlockIndex(int blockIndex);

    public abstract Builder setTotalBlockCount(int blockIndex);

    public abstract Builder setApplicantId(long applicantId);

    public abstract Builder setProgramTitle(String programTitle);

    public abstract Builder setProgramId(long programId);

    public abstract Builder setBlock(Block block);

    public abstract Builder setPreferredLanguageSupported(boolean preferredLanguageSupported);

    public abstract Builder setApplicantStorageClient(
        ApplicantStorageClient applicantStorageClient);

    public abstract Builder setBaseUrl(String baseUrl);

    public abstract Builder setErrorDisplayMode(
        ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode);

    public abstract Builder setBannerMessage(Optional<ToastMessage> banner);

    public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

    public abstract Builder setApplicantSelectedQuestionName(Optional<String> questionName);

    public abstract Builder setApplicantRoutes(ApplicantRoutes applicantRoutes);

    public abstract Builder setProfile(CiviFormProfile profile);

    public abstract ApplicationBaseViewParams build();
  }
}
