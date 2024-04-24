package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;

/** Renders a list of programs that an applicant can browse, with buttons for applying. */
public class NorthStarProgramIndexView extends NorthStarApplicantBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  NorthStarProgramIndexView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
  }

  public String render(
      Messages messages,
      Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      CiviFormProfile profile) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);

    context.setVariable(
        "cards",
        programCardsSectionParamsFactory.getCards(
            applicationPrograms.allPrograms(),
            /* preferredLocale= */ messages.lang().toLocale(),
            profile,
            applicantId));
    return templateEngine.process("applicant/ProgramIndexTemplate", context);
  }
}
