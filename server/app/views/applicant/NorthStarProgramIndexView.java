package views.applicant;

import org.thymeleaf.TemplateEngine;

import com.google.inject.Inject;

import controllers.AssetsFinder;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import play.mvc.Http.Request;
import views.ApplicationBaseViewParams;

public class NorthStarProgramIndexView extends NorthStarApplicantBaseView {
    @Inject
    NorthStarProgramIndexView(
        TemplateEngine templateEngine,
        ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
        AssetsFinder assetsFinder,
        ApplicantRoutes applicantRoutes) {
      super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
    }
  
    public String render(Request request) {
      ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);
      return templateEngine.process("applicant/ProgramIndexTemplate", context);
    }
}
