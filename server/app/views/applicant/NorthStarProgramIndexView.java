package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.util.Optional;
import models.LifecycleStage;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramSectionParams;

/** Renders a list of programs that an applicant can browse, with buttons for applying. */
public class NorthStarProgramIndexView extends NorthStarBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  NorthStarProgramIndexView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory,
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
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
  }

  public String render(
      Messages messages,
      Request request,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      Optional<String> bannerMessage,
      Optional<CiviFormProfile> profile) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(request, applicantId, profile, personalInfo, messages);

    ImmutableList.Builder<ProgramSectionParams> sectionParamsBuilder = ImmutableList.builder();

    Optional<ProgramSectionParams> intakeSection = Optional.empty();

    if (applicationPrograms.commonIntakeForm().isPresent()) {
      intakeSection =
          Optional.of(
              getCommonIntakeFormSection(
                  messages,
                  request,
                  applicationPrograms.commonIntakeForm().get(),
                  profile,
                  applicantId,
                  personalInfo));
    }

    if (!applicationPrograms.inProgress().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED),
              MessageKey.BUTTON_CONTINUE,
              applicationPrograms.inProgress(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId,
              personalInfo));
    }

    if (!applicationPrograms.submitted().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_SUBMITTED),
              MessageKey.BUTTON_EDIT,
              applicationPrograms.submitted(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId,
              personalInfo));
    }

    if (!applicationPrograms.unapplied().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED),
              MessageKey.BUTTON_APPLY,
              applicationPrograms.unapplied(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId,
              personalInfo));
    }

    context.setVariable("commonIntakeSection", intakeSection);
    context.setVariable(
        "numPrograms",
        applicationPrograms.inProgress().size()
            + applicationPrograms.submitted().size()
            + applicationPrograms.unapplied().size());

    context.setVariable("sections", sectionParamsBuilder.build());
    context.setVariable(
        "authProviderName",
        // The applicant portal name should always be set (there is a
        // default setting as well).
        settingsManifest.getApplicantPortalName(request).get());
    context.setVariable("createAccountLink", routes.LoginController.register().url());
    context.setVariable("isGuest", personalInfo.getType() == GUEST);
    context.setVariable("hasProfile", profile.isPresent());

    // Toasts
    context.setVariable("bannerMessage", bannerMessage);

    return templateEngine.process("applicant/ProgramIndexTemplate", context);
  }

  private ProgramSectionParams getCommonIntakeFormSection(
      Messages messages,
      Request request,
      ApplicantProgramData commonIntakeForm,
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo) {
    Optional<LifecycleStage> commonIntakeFormApplicationStatus =
        commonIntakeForm.latestApplicationLifecycleStage();

    MessageKey buttonText = MessageKey.BUTTON_START_HERE;
    if (commonIntakeFormApplicationStatus.isPresent()) {
      switch (commonIntakeFormApplicationStatus.get()) {
        case ACTIVE:
          buttonText = MessageKey.BUTTON_EDIT;
          break;
        case DRAFT:
          buttonText = MessageKey.BUTTON_CONTINUE;
          break;
        default:
          // Leave button text as is.
      }
    }

    return programCardsSectionParamsFactory.getSection(
        request,
        messages,
        Optional.empty(),
        buttonText,
        ImmutableList.of(commonIntakeForm),
        /* preferredLocale= */ messages.lang().toLocale(),
        profile,
        applicantId,
        personalInfo);
  }
}
