package views.applicant;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.geo.AddressSuggestionGroup;
import services.settings.SettingsManifest;
import views.ApplicationBaseViewParams;
import views.NorthStarBaseView;

public class NorthStarAddressCorrectionBlockView extends NorthStarBaseView {

  @Inject
  NorthStarAddressCorrectionBlockView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
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
            params.applicantId(),
            params.profile(),
            params.applicantPersonalInfo(),
            params.messages());
    context.setVariable("confirmAddressAction", getFormAction(params, applicantRequestedAction));
    context.setVariable("goBackAction", goBackAction(params));
    context.setVariable("addressSuggestionGroup", addressSuggestionGroup);
    context.setVariable("isEligibilityEnabled", isEligibilityEnabled);
    context.setVariable("applicationParams", params);
    return templateEngine.process("applicant/AddressCorrectionBlockTemplate", context);
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
