package views.applicant.addresscorrection;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.AlertSettings;
import services.AlertType;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.Block;
import services.geo.AddressSuggestionGroup;
import services.settings.SettingsManifest;
import views.applicant.ApplicantBaseView;
import views.applicant.blocks.ProgressBar;

public class AddressCorrectionBlockView extends ApplicantBaseView {

  // Constants used by ApplicantProgramBlocksController and ApplicantService
  // to process address correction form submissions
  public static final String USER_KEEPING_ADDRESS_VALUE = "USER_KEEPING_ADDRESS_VALUE";
  public static final String SELECTED_ADDRESS_NAME = "selectedAddress";
  public static final String ADDRESS_JSON_FIELD_NAME = "addressJson";

  @Inject
  AddressCorrectionBlockView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
  }

  public String render(
      Request request,
      Params params,
      AddressSuggestionGroup addressSuggestionGroup,
      ApplicantRequestedAction applicantRequestedAction,
      Boolean isEligibilityEnabled,
      String addressJson) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());

    String pageTitle =
        pageTitleWithBlockProgress(
            params.programTitle(),
            params.blockIndex(),
            params.blockList().size(),
            params.messages());
    context.setVariable("pageTitle", pageTitle);

    context.setVariable("programTitle", params.programTitle());
    context.setVariable("programShortDescription", params.programShortDescription());
    context.setVariable("confirmAddressAction", getFormAction(params, applicantRequestedAction));
    context.setVariable(
        "goBackAction", goBackAction(params, settingsManifest.getProgramSlugUrlsEnabled(request)));
    context.setVariable("addressSuggestionGroup", addressSuggestionGroup);
    context.setVariable("isEligibilityEnabled", isEligibilityEnabled);
    context.setVariable("applicationParams", params);
    context.setVariable("addressJson", addressJson);

    // Progress bar
    ProgressBar progressBar =
        new ProgressBar(params.blockList(), params.blockIndex(), params.messages());
    context.setVariable("progressBar", progressBar);

    boolean anySuggestions = addressSuggestionGroup.getAddressSuggestions().size() > 0;
    context.setVariable("anySuggestions", anySuggestions);

    String alertMessage =
        anySuggestions
            ? params.messages().at(MessageKey.ADDRESS_CORRECTION_FOUND_SIMILAR_LINE_2.getKeyName())
            : params.messages().at(MessageKey.ADDRESS_CORRECTION_NO_VALID_LINE_2.getKeyName());

    AlertSettings addressAlertSettings =
        new AlertSettings(
            /* show= */ true,
            Optional.of(params.messages().at(MessageKey.ADDRESS_CORRECTION_LINE_1.getKeyName())),
            alertMessage,
            AlertType.WARNING);
    context.setVariable("addressAlertSettings", addressAlertSettings);

    return templateEngine.process(
        "applicant/addresscorrection/AddressCorrectionBlockTemplate", context);
  }

  private String getFormAction(Params params, ApplicantRequestedAction applicantRequestedAction) {
    return applicantRoutes
        .confirmAddress(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.blockId(),
            params.inReview(),
            applicantRequestedAction)
        .url();
  }

  private String goBackAction(Params params, boolean programSlugUrlsEnabled) {
    if (programSlugUrlsEnabled) {
      return applicantRoutes
          .blockEditOrBlockReview(
              params.profile(),
              params.applicantId(),
              params.programSlug(),
              params.blockId(),
              params.inReview())
          .url();
    }
    return applicantRoutes
        .blockEditOrBlockReview(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.blockId(),
            params.inReview())
        .url();
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_AddressCorrectionBlockView_Params.Builder();
    }

    abstract Request request();

    abstract CiviFormProfile profile();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract Messages messages();

    abstract long programId();

    abstract String programSlug();

    abstract String programTitle();

    abstract String programShortDescription();

    abstract String blockId();

    abstract int blockIndex();

    abstract ImmutableList<Block> blockList();

    abstract boolean inReview();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo applicantPersonalInfo);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setProgramSlug(String programSlug);

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setProgramShortDescription(String programShortDescription);

      public abstract Builder setBlockId(String blockId);

      public abstract Builder setBlockIndex(int blockIndex);

      public abstract Builder setBlockList(ImmutableList<Block> blockList);

      public abstract Builder setInReview(boolean inReview);

      public abstract Params build();
    }
  }
}
