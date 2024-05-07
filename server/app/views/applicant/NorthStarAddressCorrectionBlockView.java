package views.applicant;

import org.thymeleaf.TemplateEngine;

import com.google.inject.Inject;

import controllers.AssetsFinder;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import play.mvc.Http.Request;
import services.geo.AddressSuggestionGroup;
import views.ApplicationBaseViewParams;

public class NorthStarAddressCorrectionBlockView extends NorthStarApplicantBaseView {
    
      @Inject
      NorthStarAddressCorrectionBlockView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
  }

public String render(Request request, ApplicationBaseViewParams params, AddressSuggestionGroup addressSuggestionGroup) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);
    context.setVariable("confirmAddressMethod", getFormAction(params, ApplicantRequestedAction.NEXT_BLOCK));
    context.setVariable("suggestions", addressSuggestionGroup.getAddressSuggestions());
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
}
