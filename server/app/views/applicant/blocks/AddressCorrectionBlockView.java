package views.applicant.blocks;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.AlertSettings;
import services.AlertType;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.MessageKey;
import services.geo.AddressSuggestionGroup;
import services.settings.SettingsManifest;
import views.applicant.ApplicantBaseView;
import views.applicant.ProgressBar;
import views.trustedintermediary.ApplicationBaseViewParams;

public class AddressCorrectionBlockView extends ApplicantBaseView {

  // Constants used by ApplicantProgramBlocksController and ApplicantService
  // to process address correction form submissions
  public static final String USER_KEEPING_ADDRESS_VALUE = "USER_KEEPING_ADDRESS_VALUE";
  public static final String SELECTED_ADDRESS_NAME = "selectedAddress";

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
      ApplicationBaseViewParams params,
      AddressSuggestionGroup addressSuggestionGroup,
      ApplicantRequestedAction applicantRequestedAction,
      Boolean isEligibilityEnabled) {
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
    context.setVariable("goBackAction", goBackAction(params));
    context.setVariable("addressSuggestionGroup", addressSuggestionGroup);
    context.setVariable("isEligibilityEnabled", isEligibilityEnabled);
    context.setVariable("applicationParams", params);

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

    return templateEngine.process("applicant/blocks/AddressCorrectionBlockTemplate", context);
  }

  private String getFormAction(
      ApplicationBaseViewParams params, ApplicantRequestedAction applicantRequestedAction) {
    return applicantRoutes
        .confirmAddress(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.block().getId(),
            params.inReview(),
            applicantRequestedAction)
        .url();
  }

  private String goBackAction(ApplicationBaseViewParams params) {
    return applicantRoutes
        .blockEditOrBlockReview(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.block().getId(),
            params.inReview())
        .url();
  }
}
